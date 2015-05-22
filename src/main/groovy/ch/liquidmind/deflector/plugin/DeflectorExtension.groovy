package ch.liquidmind.deflector.plugin

/**
 * Created by sebastian on 22/05/15.
 */
class DeflectorExtension {
    def deflectedJarsPath = "deflected-jars"
    def jarsToDeflect = []

    def deflect(@DelegatesTo(strategy=Closure.DELEGATE_FIRST, value=DeflectOption) Closure cl) {
        DeflectOption deflectOption = new DeflectOption()
        def code = cl.rehydrate(deflectOption, this, this)
        code.resolveStrategy = Closure.DELEGATE_FIRST
        code()

        jarsToDeflect << deflectOption
    }
}
