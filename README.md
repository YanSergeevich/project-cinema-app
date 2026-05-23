# 🎬 Cinema Management App

Java console application for cinema ticket management — 2026

## About

A Java-based console application to manage cinema operations including movie sessions, ticket sales, and customer data. Built with layered architecture and input validation.

## Features

### Core Functionality
- **Movie Management** – Add, remove, and list available movies
- **Session Scheduling** – Create and manage showtimes with hall allocation
- **Ticket System** – Purchase tickets, view booking history, calculate revenue
- **Customer Records** – Store customer information and purchase history
- **SQL Database Integration** – Persistent data storage using SQL database

### Technical Implementation
- **Layered Architecture** – Models, Repository, Service, Console UI layers
- **Exception Handling** – Custom exceptions for validation and business logic
- **File Logging** – Application logs stored in `/logs` directory
- **Input Validation** – Comprehensive checks for user inputs and data integrity
- **SQL Database** – Data persistence and query management

## Project Structure
project-cinema-app/
- **console**/ Console user interface layer
- **exception**/ Custom exception classes
- **logs**/ Application log files
- **models**/ Domain entities (Movie, Session, Ticket, etc.)
- **repository**/ Data storage and retrieval logic
- **service**/ Business logic layer
- **validation**/ Input validation utilities
- **out**/ Compiled output files

## Technologies

- **Java** – Core programming language
- **SQL** – Database management and persistent storage
- **File I/O** – Additional data persistence
- **Java Logging** – Application activity tracking

### Prerequisites
- Java JDK 11 or higher
- SQL Database (MySQL/PostgreSQL/SQLite)
- Command line terminal
