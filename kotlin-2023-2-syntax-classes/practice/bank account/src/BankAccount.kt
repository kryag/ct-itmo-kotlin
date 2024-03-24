class BankAccount(amount: Int) {
    var balance: Int = amount
        private set(value) {
            logTransaction(field, value)
            field = value
        }

    init {
        require(amount > 0) { "Incorrect amount, must be positive" }
    }

    fun deposit(amount: Int) {
        require(amount > 0) { "Incorrect amount, must be positive" }
        balance += amount
    }

    fun withdraw(amount: Int) {
        require(amount <= balance) { "Withdrawal amount exceeds balance" }
        require(amount > 0) { "Incorrect amount, must be positive" }
        balance -= amount
    }
}
