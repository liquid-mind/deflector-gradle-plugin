package ch.liquidmind.deflector.plugin
import java.util.regex.Pattern

/**
 * Created by sebastian on 22/05/15.
 */
class DeflectOption {
    private options = [:]

    void jar(String jar) {
        options['--jar'] = jar
    }

    void includes(Object includes) {
        options['--includes'] = includes.toString()
    }

    void excludes(Object excludes) {
        options['--excludes'] = excludes.toString()
    }

    void classpath(String classpath) {
        options['--classpath'] = classpath
    }


    def getDeflectorArgs() {
        def args = []
        options.each { k,v ->
            args << k
            args << v
        }
        return args
    }
}
