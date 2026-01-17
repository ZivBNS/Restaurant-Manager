CREATE DATABASE IF NOT EXISTS bistro;

USE bistro;

-- 1. Users Table
CREATE TABLE Users (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    FirstName VARCHAR(25),
    LastName VARCHAR(25),
    Phone VARCHAR(14),
    Email VARCHAR(35),
    Username VARCHAR(20) UNIQUE,
    Password VARCHAR(20),
    subscriberCode INT,
    Identity ENUM('Subscriber', 'Manager', 'Employee', 'Deleted') NOT NULL
);

-- 2. Tables Table
CREATE TABLE Tables (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    TableNumber INT,
    Size INT,
    IsActive BOOLEAN DEFAULT TRUE
);

-- 3. OpeningHours Table
CREATE TABLE OpeningHours (
    DayOfWeek ENUM('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday') NOT NULL,
    OpenTime TIME,
    CloseTime TIME,
    IsActive BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (DayOfWeek, OpenTime)
);

-- 4. SpecialHours Table
CREATE TABLE SpecialHours (
    Date DATE PRIMARY KEY,
    OpenTime TIME,
    CloseTime TIME,
    Description TEXT
);

-- 5. Reservations Table (Depends on Users and Tables)
CREATE TABLE Reservations (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    UserID INT, -- Note: This allows NULL for Guest users
    TableID INT,
    Phone VARCHAR(14),
    Email VARCHAR(35),
    ReservationStartTime DATETIME,
    ReservationEndTime DATETIME,
    ActualArrivalTime DATETIME,
    ActualDepartureTime DATETIME,
    NumberOfDiners INT,
    ConfirmationCode INT,
    Status VARCHAR(25),
    CreationTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    RemindedPreArrival BOOLEAN DEFAULT FALSE,
    RemindedDeparture BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (UserID) REFERENCES Users(ID),
    FOREIGN KEY (TableID) REFERENCES Tables(ID)
);

-- 6. Waitlist Table (Depends on Reservations)
CREATE TABLE Waitlist (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    ReservationID INT UNIQUE,
    Status VARCHAR(25),
    creationTime DATETIME,
    TableFreedTime DATETIME,
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ID)
);

-- 7. Bills Table (Depends on Reservations)
CREATE TABLE Bills (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    ReservationID INT UNIQUE,
    TotalAmount DECIMAL(10, 2) NOT NULL,
    BillDetails TEXT,
    DiscountPercentage DECIMAL(5, 2) DEFAULT 0.00,
    Status VARCHAR(25),
    FOREIGN KEY (ReservationID) REFERENCES Reservations(ID)
);

-- 8. Report Management Table
CREATE TABLE IF NOT EXISTS reports_management (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    report_month INT NOT NULL,
    report_year INT NOT NULL,
    date_generated DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 9. Time Report Details Table (Depends on reports_management)
CREATE TABLE IF NOT EXISTS time_report_details (
    report_id INT,
    day_index INT NOT NULL,
    avg_lateness DOUBLE,
    avg_overstay DOUBLE,
    PRIMARY KEY (report_id, day_index),
    FOREIGN KEY (report_id) REFERENCES reports_management(report_id) ON DELETE CASCADE
);

-- 10. Subscriber Report Details Table (Depends on reports_management)
CREATE TABLE IF NOT EXISTS subscriber_report_details (
    report_id INT,
    day_index INT NOT NULL,
    total_orders INT,
    waiting_list_count INT,
    PRIMARY KEY (report_id, day_index),
    FOREIGN KEY (report_id) REFERENCES reports_management(report_id) ON DELETE CASCADE
);