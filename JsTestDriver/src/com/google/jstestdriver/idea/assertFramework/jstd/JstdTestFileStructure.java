package com.google.jstestdriver.idea.assertFramework.jstd;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.jstestdriver.idea.assertFramework.AbstractTestFileStructure;
import com.google.jstestdriver.idea.assertFramework.JstdRunElement;
import com.google.jstestdriver.idea.util.JsPsiUtils;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import net.jcip.annotations.NotThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@NotThreadSafe
public class JstdTestFileStructure extends AbstractTestFileStructure {

  public static final Key<String> TEST_ELEMENT_NAME_KEY = Key.create("jstd-test-element-name-key");
  public static final Key<Boolean> PROTOTYPE_TEST_DEFINITION_KEY = Key.create("jstd-prototype-test-definition-key");

  private final List<JstdTestCaseStructure> myTestCaseStructures;
  private final Map<String, JstdTestCaseStructure> myTestCaseStructureByNameMap;

  public JstdTestFileStructure(@NotNull JSFile jsFile) {
    super(jsFile);
    myTestCaseStructures = Lists.newArrayList();
    myTestCaseStructureByNameMap = Maps.newHashMap();
  }

  @Override
  public boolean isEmpty() {
    for (JstdTestCaseStructure testCaseStructure : myTestCaseStructures) {
      if (testCaseStructure.getTestCount() > 0) {
        return false;
      }
    }
    return true;
  }

  public List<JstdTestCaseStructure> getTestCaseStructures() {
    return myTestCaseStructures;
  }

  public JstdTestCaseStructure getTestCaseStructureByName(String testCaseName) {
    return myTestCaseStructureByNameMap.get(testCaseName);
  }

  public void addTestCaseStructure(JstdTestCaseStructure testCaseStructure) {
    myTestCaseStructures.add(testCaseStructure);
    myTestCaseStructureByNameMap.put(testCaseStructure.getName(), testCaseStructure);
  }

  public int getTestCaseCount() {
    return myTestCaseStructures.size();
  }

  @Override
  @Nullable
  public JstdRunElement findJstdRunElement(@NotNull TextRange textRange) {
    for (JstdTestCaseStructure testCaseStructure : myTestCaseStructures) {
      JstdRunElement jstdRunElement = testCaseStructure.findJstdRunElement(textRange);
      if (jstdRunElement != null) {
        return jstdRunElement;
      }
    }
    return null;
  }

  @Override
  public PsiElement findPsiElement(@NotNull String testCaseName, @Nullable String testMethodName) {
    JstdTestCaseStructure testCaseStructure = myTestCaseStructureByNameMap.get(testCaseName);
    if (testCaseStructure == null) {
      return null;
    }
    if (testMethodName == null) {
      return testCaseStructure.getEnclosingCallExpression();
    }
    JstdTestStructure testStructure = testCaseStructure.getTestStructureByName(testMethodName);
    if (testStructure != null) {
      return testStructure.getTestMethodNameDeclaration();
    }
    return null;
  }

  @Nullable
  public JstdTestCaseStructure findEnclosingTestCaseByOffset(int documentOffset) {
    for (JstdTestCaseStructure testCaseStructure : myTestCaseStructures) {
      TextRange testCaseCallExpressionTextRange = testCaseStructure.getEnclosingCallExpression().getTextRange();
      if (JsPsiUtils.containsOffsetStrictly(testCaseCallExpressionTextRange, documentOffset)) {
        return testCaseStructure;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public List<String> getTopLevelElements() {
    if (myTestCaseStructures.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> out = new ArrayList<String>(myTestCaseStructures.size());
    for (JstdTestCaseStructure structure : myTestCaseStructures) {
      out.add(structure.getName());
    }
    return out;
  }

  @NotNull
  @Override
  public List<String> getChildrenOf(@NotNull String topLevelElementName) {
    JstdTestCaseStructure testCaseStructure = myTestCaseStructureByNameMap.get(topLevelElementName);
    if (testCaseStructure == null) {
      return Collections.emptyList();
    }
    List<String> out = new ArrayList<String>(testCaseStructure.getTestCount());
    for (JstdTestStructure testStructure : testCaseStructure.getTestStructures()) {
      out.add(testStructure.getName());
    }
    return out;
  }

  @Override
  public boolean contains(@NotNull String testCaseName, @Nullable String testMethodName) {
    return findPsiElement(testCaseName, testMethodName) != null;
  }
}
