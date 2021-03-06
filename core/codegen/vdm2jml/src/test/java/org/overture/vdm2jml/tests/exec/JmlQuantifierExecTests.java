package org.overture.vdm2jml.tests.exec;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.overture.codegen.utils.GeneralUtils;
import org.overture.vdm2jml.tests.util.TestUtil;

@RunWith(Parameterized.class)
public class JmlQuantifierExecTests extends JmlExecTestBase
{
	public static final String TEST_DIR = JmlExecTestBase.TEST_RES_DYNAMIC_ANALYSIS_ROOT
			+ "quantifier";

	public static final String PROPERTY_ID = "quantifier";

	public JmlQuantifierExecTests(File inputFile)
	{
		super(inputFile);
	}

	@Override
	protected List<String> getSkippedTestsNames()
	{
		return Arrays.asList("Exists1.vdmsl");
	}

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data()
	{
		return TestUtil.collectVdmslFiles(GeneralUtils.getFilesRecursively(new File(TEST_DIR)));
	}

	protected String getPropertyId()
	{
		return PROPERTY_ID;
	}
}
