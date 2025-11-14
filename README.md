# Recipe-worker
This project is a Recipe Publishing System built using Spring Boot, JWT Authentication, and Role-Based Access Control (RBAC)

1️⃣ API Service (You have this now)

Handles:

User registration & login (JWT)

Role-based authorization (USER / CHEF / ADMIN)

Recipe submission via REST API

Image upload (multipart → base64)

2️⃣ Worker Service


Decodes images

Stores recipes, chefs, and image metadata into database
