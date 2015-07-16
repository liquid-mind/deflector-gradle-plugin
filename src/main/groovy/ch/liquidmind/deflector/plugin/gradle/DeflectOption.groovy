package ch.liquidmind.deflector.plugin.gradle

/**
 * Created by sebastian on 22/05/15.
 */
class DeflectOption {
    // Map of Key: --<option> Value: 'someparameter'
    private nativeDeflectorOptions = [:]
    public dependsOn = []

    // Dependencies
    public void dependsOn(String... list)
    {
        list.each {
            dependsOn << it
        }
    }

    // Used to add nativeDeflectorOptions
    // Adds a --<methodname> <param> option
    // when calling any undefined method on this class
    def methodMissing(String name, args) {
        nativeDeflectorOptions.put('-' + name, args[0])
    }

    def String getDeflectorOption(String option)
    {
        return nativeDeflectorOptions.get('-' + option)
    }

    // Returns array version of the nativeDeflectorOptions map
    def String[] getDeflectorArgs() {
        def args = []
        nativeDeflectorOptions.each { k, v ->
            args << k
            args << "\"${v}\""
        }
        return args as String[]
    }
}
