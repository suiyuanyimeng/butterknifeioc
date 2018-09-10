package Utils;

import View.FindViewByIdDialog;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.command.WriteCommandAction.Simple;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import entity.Element;
import org.apache.http.util.TextUtils;

import java.util.List;

public class ViewFieldMethodCreator extends Simple {

    private FindViewByIdDialog mDialog;
    private Editor mEditor;
    private PsiFile mFile;
    private Project mProject;
    private PsiClass mClass;
    private List<Element> mElements;
    private PsiElementFactory mFactory;

    public ViewFieldMethodCreator(FindViewByIdDialog dialog, Editor editor, PsiFile psiFile, PsiClass psiClass, String command, List<Element> elements, String selectedText) {
        super(psiClass.getProject(), command);
        mDialog = dialog;
        mEditor = editor;
        mFile = psiFile;
        mProject = psiClass.getProject();
        mClass = psiClass;
        mElements = elements;
        // 获取Factory
        mFactory = JavaPsiFacade.getElementFactory(mProject);
    }

    @Override
    protected void run() throws Throwable {
        try {
            generateFields();
            generateOnClickMethod();
        } catch (Exception e) {
            // 异常打印
            mDialog.cancelDialog();
            Util.showPopupBalloon(mEditor, e.getMessage(), 10);
            return;
        }
        // 重写class
        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mProject);
        styleManager.optimizeImports(mFile);
        styleManager.shortenClassReferences(mClass);
        new ReformatCodeProcessor(mProject, mClass.getContainingFile(), null, false).runWithoutProgress();
        Util.showPopupBalloon(mEditor, "生成成功", 5);
    }

    /**
     * 创建变量
     */
    private void generateFields() {
        for (Element element : mElements) {
            if (mClass.getText().contains("@BindView(" + element.getFullID() + ")")) {
                // 不创建新的变量
                continue;
            }
            StringBuilder fromText = new StringBuilder();
            String fieldJTextFieldLabel = element.getAnnotationForId();
            if (!TextUtils.isEmpty(fieldJTextFieldLabel)) {
                // 创建注释
                fromText.append("/*** ").append(fieldJTextFieldLabel).append(" **/\n");
            }
            // 创建 注解和控件名称、变量 即 @BindView(R.id.xxx) View xxx
            fromText.append("@BindView(").append(element.getFullID()).append(")\n");
            fromText.append(element.getName());
            fromText.append(" ");
            fromText.append(element.getFieldName());
            fromText.append(";");
            // 创建点击方法
            if (element.isCreateFiled()) {
                // 添加到class
                mClass.add(mFactory.createFieldFromText(fromText.toString(), mClass));
            }
        }
    }


    /**
     * 创建OnClick方法
     */
    private void generateOnClickMethod() {
        for (Element element : mElements) {
            // 可以使用并且可以点击
            if (element.isCreateClickMethod()) {
                // 需要创建OnClick方法
                String methodName = getClickMethodName(element) + "Click";
                PsiMethod[] onClickMethods = mClass.findMethodsByName(methodName, true);
                boolean clickMethodExist = onClickMethods.length > 0;
                if (!clickMethodExist) {
                    // 创建点击方法
                    createClickMethod(methodName, element);
                }
            }
        }
    }

    /**
     * 创建一个点击事件
     */
    private void createClickMethod(String methodName, Element element) {
        // 拼接方法的字符串
        StringBuilder methodBuilder = new StringBuilder();
        methodBuilder.append("@OnClick(" + element.getFullID() + ")\n");
        methodBuilder.append("public void " + methodName + "(" + element.getName() + " " + getClickMethodName(element) + "){");
        methodBuilder.append("\n}");
        // 创建OnClick方法
        mClass.add(mFactory.createMethodFromText(methodBuilder.toString(), mClass));
    }

    /**
     * 获取点击方法的名称
     */
    public String getClickMethodName(Element element) {
        String[] names = element.getId().split("_");
        // aaBbCc
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i == 0) {
                sb.append(names[i]);
            } else {
                sb.append(Util.firstToUpperCase(names[i]));
            }
        }
        return sb.toString();
    }
}
