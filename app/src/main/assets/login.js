document.getElementById("loginForm").addEventListener("submit", function(event) {
    event.preventDefault();

    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;

    if (username === "admin" && password === "password") {
        alert("Login successful!");

        // Set session cookie
        var expirationTime = new Date().getTime() + (10 * 60 * 1000); // Current time + 10 minutes
        document.cookie = "session=" + expirationTime;

        // Redirect to /home
        window.location.href = "/home";
    } else {
        alert("Invalid username or password");
    }
});