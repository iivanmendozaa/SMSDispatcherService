document.getElementById("settingsForm").addEventListener("submit", function(event) {
    event.preventDefault();

    var formData = new FormData(this);

    formData.forEach((value, key) => {
        fetch(`/modifySettings?key=${key}&value=${value}`, {
            method: "POST",
            headers: {
                "x-api-key": "YOUR_API_KEY_HERE" // Replace with your API key
            }
        })
        .then(response => response.text())
        .then(data => {
            document.getElementById("statusMessage").textContent = data;
        })
        .catch(error => {
            console.error("Error saving settings:", error);
            document.getElementById("statusMessage").textContent = "Error saving settings";
        });
    });
});

window.onload = function() {
    fetch("/rawSettings", {
        headers: {
            "x-api-key": "YOUR_API_KEY_HERE" // Replace with your API key
        }
    })
    .then(response => response.json())
    .then(settings => {
        document.getElementById("mongoDbConnectionString").value = settings.mongoDbConnectionString || "";
        document.getElementById("msSQLConnectionString").value = settings.msSQLConnectionString || "";
        document.getElementById("msSQLUser").value = settings.msSQLUser || "";
        document.getElementById("msSQLPassword").value = settings.msSQLPassword || "";
        document.getElementById("queryInterval").value = settings.queryInterval || "";

        document.getElementById("rabbitHost").value = settings.rabbitHost || "";
        document.getElementById("rabbitUser").value = settings.rabbitUser || "";
        document.getElementById("rabbitPassword").value = settings.rabbitPassword || "";
    })
    .catch(error => {
        console.error("Error retrieving settings:", error);
    });
};
