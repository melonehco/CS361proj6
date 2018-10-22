package proj6AbulhabFengMaoSavillo;


import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JavaParser {
    public static void main(String[] args) {

        List<String> methods = Arrays.asList("luke@gmail.com",
                "andy@yahoocom", "34234sdfa#2345", "f344@gmail.com",
                "private void using(){return null;}", "public void this(String this){return this;}");

        String methodRegex = "((public|private|protected|static|final|native|synchronized|abstract|transient)+\\s)+[\\$_\\w\\<\\>\\[\\]]*\\s+[\\$_\\w]+\\([^\\)]*\\)?\\s*\\{?[^\\}]*\\}?";

        Pattern p = Pattern.compile(methodRegex);

        for (String method : methods) {

            Matcher m = p.matcher(method);

            if (m.matches()) {
                System.out.printf("%s matches%n", method);
            } else {
                System.out.printf("%s does not match%n", method);
            }
        }
    }
}
