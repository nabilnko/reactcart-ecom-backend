# ReactCart E-Commerce - Backend API

A simple Spring Boot backend for an e-commerce store. Handles user authentication, products, orders, and admin features.

## What Does This Do?

CUSTOMER FEATURES:
✓ Sign up and login
✓ Browse products
✓ Add products to cart
✓ Place orders
✓ Write reviews
✓ View order history

ADMIN FEATURES:
✓ Add/edit/delete products
✓ Upload product images
✓ Manage categories
✓ View all orders
✓ Manage customers
✓ See sales analytics

## What You Need to Install

1. Java 17 or higher
   Download from: https://www.oracle.com/java/technologies/downloads/

2. Maven (for building the project)
   Download from: https://maven.apache.org/download.cgi

3. MySQL 8.0 or higher
   Download from: https://www.mysql.com/downloads/

## Setup Instructions (Step by Step)

### STEP 1: Create Database
Open MySQL and run this command:
CREATE DATABASE ecommerce_db;

### STEP 2: Download and Open the Project
git clone https://github.com/nabilnko/reactcart-ecom-backend.git
cd reactcart-ecom-backend

### STEP 3: Set Database Credentials
Open this file: src/main/resources/application.properties

Find these lines and update with YOUR MySQL username and password:
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

### STEP 4: Install Dependencies
Open command prompt in project folder and run:
mvn clean install

### STEP 5: Start the Backend
mvn spring-boot:run

You should see: "Started EcommerceApplication" ✓

Backend is now running on: http://localhost:8080

### STEP 6: Create First Admin Account
Use Postman or any API tool to send:

POST to: http://localhost:8080/api/auth/register

Send this data:
{
  "name": "Admin User",
  "email": "admin@shophub.com",
  "password": "admin123",
  "role": "ADMIN"
}

## Main Features

AUTHENTICATION:
- Login endpoint: POST /api/auth/login
- Register endpoint: POST /api/auth/register

PRODUCTS:
- Get all products: GET /api/products
- Get product by ID: GET /api/products/{id}
- Create product: POST /api/products (Admin only)
- Update product: PUT /api/products/{id} (Admin only)
- Delete product: DELETE /api/products/{id} (Admin only)
- Upload image: POST /api/products/upload-image (Admin only)

ORDERS:
- Get all orders: GET /api/orders (Admin only)
- Get my orders: GET /api/orders/user/{userId}
- Place order: POST /api/orders
- Update status: PUT /api/orders/{id}/status (Admin only)

REVIEWS:
- Get reviews: GET /api/reviews/product/{productId}
- Write review: POST /api/reviews
- Delete review: DELETE /api/reviews/{id}

USERS:
- Get all users: GET /api/users (Admin only)
- Get user: GET /api/users/{id}
- Update profile: PUT /api/users/{id}

## Technology Used

Java 17 - Programming language
Spring Boot 3.x - Framework
MySQL - Database
JWT - Security tokens
Maven - Build tool

## Troubleshooting

Problem: "Connection refused to MySQL"
Solution: Make sure MySQL is running and credentials are correct

Problem: "Port 8080 already in use"
Solution: Change port in application.properties:
server.port=8081

Problem: "Maven not found"
Solution: Make sure Maven is installed and added to system PATH

## Need Help?

GitHub: https://github.com/nabilnko/reactcart-ecom-backend
