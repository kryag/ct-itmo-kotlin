import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Config(config: String) {
    private val map: MutableMap<String, Any> = HashMap()

    init {
        (getResource(config) ?: throw IllegalArgumentException("File not found")).reader().forEachLine {
            val entry = it.split("=")
            map[entry[0].trim()] = entry[1].trim()
        }
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, Any> {
        require(map.containsKey(property.name)) { "Key not found" }
        return ReadOnlyProperty { _, _ -> map.getValue(property.name) }
    }
}
