fun mapSquares(values: IntArray): IntArray {
    return IntArray(values.size) { index: Int -> values[index] * values[index] }
}
