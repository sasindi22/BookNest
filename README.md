# 📚 BookNest — Android E-Commerce Book Store

![Platform](https://img.shields.io/badge/platform-Android-green)
![Payment](https://img.shields.io/badge/payment-PayHere-blue)
![Status](https://img.shields.io/badge/status-active-brightgreen)

**BookNest** is a native **Android e-commerce application** built using **Java, XML, Firebase, and the PayHere Android SDK**.
It supports user authentication, book browsing, cart management, secure checkout with PayHere (sandbox), order processing, and stock management using cloud-based services.

---

## 📌 Features

### **User Features**

* ✅ User registration and login using Firebase Authentication
* ✅ Persistent login using SharedPreferences
* ✅ Browse book categories and available books
* ✅ View book details (title, price, stock availability)
* ✅ Add books to cart with quantity selection
* ✅ View and manage shopping cart
* ✅ Secure checkout using **PayHere payment gateway (Sandbox mode)**
* ✅ Automatic order creation after successful payment
* ✅ Cart cleared automatically after checkout
* ✅ View total price before payment

### **Order & Payment Features**

* ✅ PayHere Android SDK integration
* ✅ Client-side payment initiation
* ✅ Payment success & failure handling
* ✅ Order storage in Firebase Firestore
* ✅ Automatic stock quantity reduction after purchase

### **Admin / Management Features**

* ✅ Add and manage book categories
* ✅ Add, update, and manage books
* ✅ Manage stock quantities
* ✅ View customer orders
* ✅ Track purchased book quantities

> *Admin features are basic and intended for academic use.*

---

## 🛠️ Technologies Used

| Layer         | Stack                                       |
| ------------- | ------------------------------------------- |
| Platform      | Android                                     |
| Language      | Java                                        |
| UI Design     | XML, Material Components                    |
| Architecture  | Activities, Fragments, RecyclerView         |
| Backend       | Firebase Authentication, Firebase Firestore |
| Storage       | Firebase Storage                            |
| Payments      | PayHere Android SDK (Sandbox)               |
| Image Loading | Glide                                       |
| Local Storage | SharedPreferences                           |
| Build Tool    | Gradle                                      |
| IDE           | Android Studio                              |

---

## 📁 Project Structure

```text
BookNest/
│
├── activities/
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── MainActivity.java
│
├── ui/
│   ├── customer/
│   │   ├── CustomerCheckoutFragment.java
│   │   ├── CustomerCartFragment.java
│   │   ├── CustomerHomeFragment.java
│
├── adapters/
│   ├── BookAdapter.java
│   ├── CartAdapter.java
│
├── models/
│   ├── Book.java
│   ├── Category.java
│   ├── User.java
│
├── utils/
│   ├── SessionManager.java
│
├── res/
│   ├── layout/
│   ├── drawable/
│   ├── values/
│
├── AndroidManifest.xml
├── build.gradle
└── google-services.json
```

---

## 🗃️ Firebase Database

### **Firestore Collections**

```text
users/
  └── userId
       ├── fname
       ├── lname
       ├── email
       ├── mobile
       ├── address

books/
  └── bookId
       ├── title
       ├── price
       ├── quantity
       ├── categoryId
       ├── imageUrl

cart/
  └── cartId
       ├── userId
       ├── bookId
       ├── quantity

orders/
  └── orderId
       ├── userId
       ├── bookDetails
       ├── total
       ├── address
       ├── date
       ├── status
```

---

## 🚀 Getting Started

### **1. Clone the Project**

```bash
git clone https://github.com/your-username/BookNest.git
```

### **2. Open in Android Studio**

* Open **Android Studio**
* Select **Open Existing Project**
* Choose the `BookNest` directory

### **3. Firebase Setup**

1. Create a Firebase project
2. Add an Android app with your package name
3. Download `google-services.json`
4. Place it inside the `app/` folder
5. Enable:

   * Firebase Authentication (Email/Password)
   * Firebase Firestore
   * Firebase Storage

### **4. PayHere Configuration**

* Use PayHere **Sandbox Merchant ID**
* Set sandbox URL:

```java
PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
```

### **5. Run the Application**

* Sync Gradle
* Run on emulator or physical device

---

## ⚠️ Known Limitations

* Payment verification is client-side only
* No server-side payment validation (no webhooks)
* Sandbox payment environment only
* No refund or retry mechanism
* Stock updates are not transactional
* Not production-ready (educational project)

---

## 🚧 Future Improvements

* Server-side payment verification (Cloud Functions)
* MVVM architecture with ViewModel
* Offline caching
* Order history screen
* Payment retry & refund handling
* Advanced admin dashboard
* Atomic stock updates using transactions

---

## 📄 License

This project is developed **for educational purposes only**.
Not intended for commercial deployment.

---

## 👨‍💻 Author

Created by **Thedara Sasindi**  
*Ungergraduate Full‑stack Software Engineering*  
GitHub: <https://github.com/sasindi22>  
Email: thedarasasindi@gmail.com
