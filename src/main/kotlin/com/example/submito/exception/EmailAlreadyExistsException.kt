package com.example.submito.exception

class EmailAlreadyExistsException(val email: String) :
        RuntimeException("Email $email already exists")
