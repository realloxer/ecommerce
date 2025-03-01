# 🛒 Spring-based eCommerce

This is a fully functional **eCommerce system** built with **Spring Boot**, following best practices for database design, code architecture, and security. The system allows **guest users** to browse products, create orders, and complete payments using **PayTabs** integration.

---

## 📋 Features

**1. My Orders**

- Display a list of all past orders.
- Show detailed information for each order, including status and payment information.

**2. Order Page Details**
- Display the payment request and response payloads.
For refunded orders, display refund history with request and response payloads.

**3. Create New Order**
- Guest users can add products with quantities.
- Users can view available products preloaded in the database.
- Navigate to a checkout page to finalize the purchase.

**4. Checkout**
- Users can enter personal information for payment processing.
- Using PayTabs' iFrame Payment Mode within AJAX loading and without page redirection.
- Redirect the user to a success or error page based on payment result.

---

## 🛠️ Technologies Used

- **Spring Boot**: Core framework for the application.
- **Spring Data JPA**: Database interaction using repositories.
- **Thymeleaf**: Server-side templating for rendering UI.
- **PayTabs**: Payment gateway for secure transactions.
- **PostgreSQL**: Database to store products, orders, and payments.
- **Lombok**: Simplifying boilerplate code with annotations.

---

## 🗄️ Database Schema

### Order
| Field          | Type         | Description                    |
|----------------|--------------|--------------------------------|
| id             | UUID         | Primary Key                    |
| status         | Enum         | PENDING, COMPLETED, REFUNDED   |
| totalAmount    | BigDecimal   | Order total                    |
| name, email, address | String  | Customer details               |
| shippingMethod | Enum         | DELIVERY, PICKUP               |

### OrderItem
| Field    | Type         | Description            |
|----------|--------------|------------------------|
| id       | UUID         | Primary Key            |
| order    | Relationship | Reference to Order     |
| product  | Relationship | Reference to Product   |
| quantity | Integer      | Product quantity       |
| price    | BigDecimal   | Item price             |

### Payment
| Field         | Type         | Description                    |
|---------------|--------------|--------------------------------|
| id            | Long         | Primary Key                    |
| amount        | BigDecimal   | Payment amount                 |
| paymentDate   | LocalDate    | Date of payment                |
| paymentMethod | String       | e.g., Visa, Mastercard         |
| order         | Relationship | Reference to Order             |

### Product
| Field       | Type       | Description                     |
|-------------|------------|---------------------------------|
| id          | UUID       | Primary Key                     |
| name        | String     | Product name                    |
| description | String     | Product description             |
| price       | BigDecimal | Product price                   |

### RefundRecord
| Field           | Type         | Description               |
|-----------------|--------------|---------------------------|
| id              | UUID         | Primary Key               |
| order           | Relationship | Reference to Order        |
| requestPayload  | JSON         | Refund request payload    |
| responsePayload | JSON         | Refund response payload   |

---

## 🔧 How to Set Up and Run

### 1. Clone the Repository

```bash
git clone REPO_URL
cd ecommerce
```
### 2. Configure Database
Ensure PostgreSQL is installed and running.

Update these fields in **application.yml**:

```yml
url: jdbc:postgresql://localhost:5432/ecommerce_db
username: postgres
password: 123456
```

### 3. Add Products to Database
Add products into the database, example script:
```sql
INSERT INTO products (id, name, description, price) VALUES
    (gen_random_uuid(), 'Smartphone', 'Latest 5G smartphone with 128GB storage.', 699.99),
    (gen_random_uuid(), 'Laptop', 'Ultra-thin laptop with 16GB RAM and 512GB SSD.', 1299.99),
    (gen_random_uuid(), 'Wireless Headphones', 'Noise-canceling wireless headphones.', 199.99),
    (gen_random_uuid(), 'Smartwatch', 'Waterproof smartwatch with heart rate monitor.', 249.99),
    (gen_random_uuid(), 'Gaming Console', 'Next-gen gaming console with 1TB storage.', 499.99);
```

### 4. Run the Application
```
mvn spring-boot:run
```

**Note**: If you want to test payment redirection to success and error page, please deploy the app to an endpoint with https (we can use [ngrok](https://ngrok.com/)) and change these URLs in `PaymentController.java`:
```java
body.put("return", "https://5d43-116-109-30-201.ngrok-free.app/payment/redirect");
body.put("callback", "https://5d43-116-109-30-201.ngrok-free.app/payment/callback")
```

##  📌 Usage
### 1. Access the Product List
- Navigate to /products
- Select products and quantities
- Proceed to the /create page

### 2. Create an Order
- Choose the shipping method (Delivery or Pickup)
- Proceed to /checkout for payment

### 3. Make Payment
- Enter your details (name, email, address)
- Complete payment via PayTabs iFrame

### 4. View Orders
- Access /orders to see all previous orders.
- Click an order to view payment and refund details.

## 🔒 Security Considerations
- Input Validation: Validates all user inputs before storing.
- UUID Usage: Uses secure UUIDs to prevent enumeration.
- Secure Payment Integration: Leverages PayTabs iFrame to avoid handling sensitive card information.
- Transaction Management: Ensures database consistency using @Transactional.
- Protected against SQL injection due to the use of Spring Data JPA and the Repository.

## Improvements
- Add Unit Tests.
- CORS Protection: Restricts access to known domains.
- Enhance error logging and monitoring.