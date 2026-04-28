CREATE DATABASE IF NOT EXISTS securemal;
USE securemal;

-- Users table stores authentication info and timestamps
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Files table keeps track of uploaded malware samples and their metadata
CREATE TABLE IF NOT EXISTS files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Reports table contains the plain-English analysis findings and technical details
CREATE TABLE IF NOT EXISTS reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    file_id INT,
    md5_hash VARCHAR(32),
    sha256_hash VARCHAR(64),
    file_type VARCHAR(255),
    risk_score INT,
    risk_label VARCHAR(50),
    plain_summary TEXT,
    timeline TEXT,  -- Storing JSON array string as TEXT
    suspicious_strings TEXT,
    pe_info TEXT,  -- Storing PE details
    analysis_type VARCHAR(50),
    raw_result TEXT, -- Full original JSON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE
);
