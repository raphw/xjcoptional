package codes.rafael.xjcoptional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSParticle;

public class OptionalGettersPlugin extends Plugin {

    public String getOptionName() {
        return "Xnullsafegetters";
    }

    public String getUsage() {
        return "Changes getters for optional, non-repeated properties to return java.util.Optional values.";
    }

    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) {
        JCodeModel codeModel = outline.getCodeModel();
        JClass optional = codeModel.ref("java.util.Optional");
        JInvocation ofNullable = optional.staticInvoke("ofNullable");
        for (ClassOutline classOutline : outline.getClasses()) {
            for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
                XSComponent component = fieldOutline.getPropertyInfo().getSchemaComponent();
                if (component instanceof XSParticle) {
                    XSParticle pt = (XSParticle) component;
                    if (pt.getMinOccurs().equals(BigInteger.ZERO) && pt.getMaxOccurs().equals(BigInteger.ONE)) {
                        String getter = "get" + fieldOutline.getPropertyInfo().getName(true);
                        JMethod existing = classOutline.implClass.getMethod(getter, new JType[0]);
                        if (existing != null) {
                            List<JMethod> methods = new ArrayList<JMethod>(classOutline.implClass.methods());
                            classOutline.implClass.methods().clear(); // To restore original method order after.
                            for (JMethod method : methods) {
                                if (method == existing) {
                                    JMethod m = classOutline.implClass.method(
                                        existing.mods().getValue(),
                                        optional.narrow(existing.type()),
                                        getter);
                                    String name = fieldOutline.getPropertyInfo().getName(false);
                                    m.javadoc()
                                        .append("Gets the value of the " + name + " property.")
                                        .addReturn().append("The optional value of " + name + ".");
                                    m.body()._return(ofNullable.arg(JExpr.ref(name)));
                                } else {
                                    classOutline.implClass.methods().add(method);
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
