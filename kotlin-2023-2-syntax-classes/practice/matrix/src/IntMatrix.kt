class IntMatrix(val rows: Int, val columns: Int) {
    private val array: IntArray

    init {
        require(rows >= 0 && columns >= 0) { "Matrix dimensions cannot be negative" }
        array = IntArray(rows * columns)
    }

    operator fun get(row: Int, column: Int): Int {
        require(row in 0..<rows && column in 0..<columns) { "Out of matrix boundaries" }
        return array[getIndex(row, column)]
    }

    operator fun set(row: Int, column: Int, value: Int) {
        require(row in 0..<rows && column in 0..<columns) { "Out of matrix boundaries" }
        array[getIndex(row, column)] = value
    }

    private fun getIndex(row: Int, column: Int): Int = row * columns + column
}
