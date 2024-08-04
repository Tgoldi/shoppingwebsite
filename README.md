# E-Commerce Backend

This is the backend for an e-commerce web application, developed as part of a full-stack project.

## Project Description

This backend provides an API for managing products, user accounts, shopping carts, and orders. Features include user authentication, product search, favorite lists, and stock management.

## Technology Stack

- Java 17
- Spring Boot
- Spring Security with JWT for authentication
- Spring Data JPA
- H2 Database
- Maven

## Key Features

1. **User Authentication:** Secure signup and login using JWT.
2. **Product Management:** CRUD operations for products with search.
3. **Shopping Cart:** Add/remove items, manage quantities.
4. **Order Processing:** Place orders with a limit of 2 items per product.
5. **Favorite List:** Maintain a list of favorite products.
6. **Stock Management:** Automatic updates when orders are placed.

## Project Structure

- `config`: Configuration classes for CORS and JWT.
- `controller`: REST API endpoints.
- `dto`: Data Transfer Objects.
- `exception`: Custom exceptions.
- `model`: Entity classes (User, Product, Order, etc.).
- `repository`: Database operation interfaces.
- `security`: JWT configuration and filters.
- `service`: Business logic.

## Setup and Running

1. Ensure Java 17 and Maven are installed.
2. Clone the repository:
    git clone [repository-url]
  
The server will start at `http://localhost:9090`.

## Testing

Run unit tests:
```bash
mvn test
```
## License

For questions or issues, please open an issue in the GitHub repository.
