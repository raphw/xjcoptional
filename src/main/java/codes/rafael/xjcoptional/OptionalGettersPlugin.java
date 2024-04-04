package codes.rafael.xjcoptional;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import org.xml.sax.ErrorHandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OptionalGettersPlugin extends Plugin {

    public String getOptionName() {
        return "Xnullsafegetters";
    }

    public String getUsage() {
        return "Changes getters for optional, non-repeated properties to return java.util.Optional values.";
    }

    private boolean isTermElementDeclWithDefaultValue(XSParticle particle) {
        XSTerm term = particle.getTerm();
        return term.isElementDecl() && term.asElementDecl().getDefaultValue() != null;
    }

    private boolean isJaxbElement(FieldOutline fieldOutline) {
        String binaryName = fieldOutline.getRawType().binaryName();
        return "javax.xml.bind.JAXBElement".equals(binaryName)
            || "jakarta.xml.bind.JAXBElement".equals(binaryName);
    }

    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) {
        JCodeModel codeModel = outline.getCodeModel();
        JClass optional = codeModel.ref("java.util.Optional");
        for (ClassOutline classOutline : outline.getClasses()) {
            for (FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
                if (isJaxbElement(fieldOutline)) {
                    continue;
                }
                boolean valid = false;
                XSComponent component = fieldOutline.getPropertyInfo().getSchemaComponent();
                if (component instanceof XSParticle) {
                    XSParticle pt = (XSParticle) component;
                    if (pt.getMinOccurs().equals(BigInteger.ZERO) && pt.getMaxOccurs().equals(BigInteger.ONE)
                        && !isTermElementDeclWithDefaultValue(pt)) {
                        valid = true;
                    }
                } else if (component instanceof XSAttributeUse) {
                    XSAttributeUse au = (XSAttributeUse) component;
                    if (!au.isRequired() && au.getDefaultValue() == null && au.getFixedValue() == null) {
                        valid = true;
                    }
                }
                if (valid) {
                    String getter = "get" + fieldOutline.getPropertyInfo().getName(true);
                    JMethod existing = classOutline.getImplClass().getMethod(getter, new JType[0]);
                    if (existing != null) {
                        List<JMethod> methods = new ArrayList<JMethod>(classOutline.getImplClass().methods());
                        classOutline.getImplClass().methods().clear(); // To restore original method order after.
                        for (JMethod method : methods) {
                            if (method == existing) {
                                JMethod m = classOutline.getImplClass().method(
                                        existing.mods().getValue(),
                                        optional.narrow(existing.type()),
                                        getter);
                                String name = fieldOutline.getPropertyInfo().getName(false);
                                m.javadoc()
                                        .append("Gets the value of the " + name + " property.")
                                        .addReturn().append("The optional value of " + name + ".");
                                m.body()._return(optional.staticInvoke("ofNullable").arg(JExpr.ref(name)));
                            } else {
                                classOutline.getImplClass().methods().add(method);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
