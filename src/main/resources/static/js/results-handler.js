const resultsModal = new bootstrap.Modal(document.getElementById('resultsModal'));

    document.querySelectorAll('.run-scan').forEach(button => {
        button.addEventListener('click', async function() {
            const dbId = this.dataset.dbId;
            const resultsDiv = document.getElementById('results-' + dbId);
            
            try {
                button.disabled = true;
                button.textContent = 'Running...';
                
                const response = await fetch(`/databases/${dbId}/scan`, {
                    method: 'POST'
                });
                
                if (response.ok) {
                    resultsDiv.innerHTML = '<div class="alert alert-success">Scan completed. Click View Results to see findings.</div>';
                } else {
                    throw new Error(await response.text());
                }
            } catch (error) {
                resultsDiv.innerHTML = `<div class="alert alert-danger">Error: ${error.message}</div>`;
            } finally {
                button.disabled = false;
                button.textContent = 'Run Security Scan';
            }
        });
    });

    document.querySelectorAll('.view-results').forEach(button => {
        button.addEventListener('click', async function() {
            const dbId = this.dataset.dbId;
            
            try {
                button.disabled = true;
                const response = await fetch(`/databases/${dbId}/results`);
                if (response.ok) {
                    const results = await response.json();
                    displayResultsInModal(results);
                    resultsModal.show(); // Show the modal after populating data
                } else {
                    throw new Error(await response.text());
                }
            } catch (error) {
                alert('Error loading results: ' + error.message);
            } finally {
                button.disabled = false;
            }
        });
    });

    function getSeverityClass(severity) {
    // Convert security-severity score to risk level
    if (severity >= 9.0) {
        return 'bg-danger'; // Critical (9.0-10.0)
    } else if (severity >= 7.0) {
        return 'bg-warning text-dark'; // High (7.0-8.9)
    } else if (severity >= 4.0) {
        return 'bg-info text-dark'; // Medium (4.0-6.9)
    } else {
        return 'bg-secondary'; // Low (0-3.9)
    }
}

function getSeverityLabel(severity) {
    if (severity >= 9.0) {
        return 'Critical';
    } else if (severity >= 7.0) {
        return 'High';
    } else if (severity >= 4.0) {
        return 'Medium';
    } else {
        return 'Low';
    }
}

function displayResultsInModal(sarif) {
    const tbody = document.querySelector('#resultsTable tbody');
    tbody.innerHTML = '';

    if (!sarif.runs || !sarif.runs[0] || !sarif.runs[0].results) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">No findings in scan results</td></tr>';
        return;
    }

    const results = sarif.runs[0].results;
    const rules = sarif.runs[0].tool.driver.rules;

    results.forEach(result => {
        const row = document.createElement('tr');
        const rule = rules.find(r => r.id === result.ruleId);
        const securitySeverity = rule.properties["security-severity"];
        const location = result.locations[0].physicalLocation;
        
        row.innerHTML = `
            <td>
                <span class="badge ${getSeverityClass(securitySeverity)}">
                    ${getSeverityLabel(securitySeverity)} (${securitySeverity})
                </span>
            </td>
            <td>${rule.shortDescription.text}</td>
            <td>${rule.properties.description}</td>
            <td>
                ${location.artifactLocation.uri}:${location.region.startLine}
                ${location.region.endLine ? '-' + location.region.endLine : ''}
            </td>
            <td>
                <button class="btn btn-sm btn-info view-code" 
                    data-location="${encodeURIComponent(JSON.stringify(location))}">
                    View Code
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });

    // Sort by severity (highest first)
    const rows = Array.from(tbody.querySelectorAll('tr'));
    rows.sort((a, b) => {
        const severityA = parseFloat(a.querySelector('.badge').textContent.match(/\(([\d.]+)\)/)[1]);
        const severityB = parseFloat(b.querySelector('.badge').textContent.match(/\(([\d.]+)\)/)[1]);
        return severityB - severityA;
    });
    
    tbody.innerHTML = '';
    rows.forEach(row => tbody.appendChild(row));
}