document.getElementById('createDbForm').addEventListener('submit', function(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const statusDiv = document.getElementById('status');
    
    statusDiv.innerHTML = '<div class="alert alert-info">Creating database... (This might take a minute)</div>';
    
    fetch('/api/codeql/create-database', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(result => {
        statusDiv.innerHTML = `<div class="alert alert-success">${result}</div>`;
    })
    .catch(error => {
        statusDiv.innerHTML = `<div class="alert alert-danger">Error: ${error}</div>`;
    });
});