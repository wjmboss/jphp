package php.runtime.reflection;

import php.runtime.common.Messages;
import php.runtime.ext.support.compile.CompileFunction;
import php.runtime.exceptions.support.ErrorType;
import php.runtime.env.Environment;
import php.runtime.env.TraceInfo;
import php.runtime.Memory;
import php.runtime.memory.support.MemoryUtils;

import java.lang.reflect.InvocationTargetException;

public class CompileFunctionEntity extends FunctionEntity {
    private final CompileFunction compileFunction;
    private MemoryUtils.Converter<?> converters[][];

    public CompileFunctionEntity(CompileFunction compileFunction) {
        super(null);
        this.compileFunction = compileFunction;
        this.setName(compileFunction.name);
    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public Memory invoke(Environment env, TraceInfo trace, Memory[] arguments) throws IllegalAccessException, InvocationTargetException {
        CompileFunction.Method method = compileFunction.find(arguments.length);
        if (method == null){
            env.warning(trace, Messages.ERR_EXPECT_LEAST_PARAMS.fetch(
                    name, compileFunction.getMinArgs(), arguments.length
            ));
            return Memory.NULL;
        } else {
            if (arguments.length > method.argsCount && !method.isVarArg()) {
                env.warning(trace, Messages.ERR_EXPECT_EXACTLY_PARAMS,
                        name, method.argsCount, arguments.length
                );
                return Memory.NULL;
            }
        }

        Class<?>[] types = method.parameterTypes;
        Object[] passed = new Object[ types.length ];

        int i = 0;
        int j = 0;
        for(Class<?> clazz : types) {
            boolean isRef = method.references[i];
            MemoryUtils.Converter<?> converter = method.converters[i];
            if (clazz == Memory.class) {
                passed[i] = isRef ? arguments[j] : arguments[j].toImmutable();
                j++;
            } else if (converter != null) {
                passed[i] = converter.run(arguments[j]);
                j++;
            } else if (clazz == Environment.class) {
                passed[i] = env;
            } else if (clazz == TraceInfo.class) {
                passed[i] = trace;
            } else if (i == types.length - 1 && types[i] == Memory[].class){
                Memory[] arg = new Memory[arguments.length - i + 1];
                if (!isRef){
                    for(int k = 0; k < arg.length; k++)
                        arg[i] = arguments[i].toImmutable();
                } else {
                    System.arraycopy(arguments, j, arg, 0, arg.length);
                }
                passed[i] = arg;
                break;
            } else {
                env.error(trace, ErrorType.E_CORE_ERROR, name + "(): Cannot call this function dynamically");
                passed[i] = Memory.NULL;
            }
            i++;
        }

        if (method.resultType == void.class){
            method.method.invoke(null, passed);
            return Memory.NULL;
        } else
            return MemoryUtils.valueOf(method.method.invoke(null, passed));
    }
}
