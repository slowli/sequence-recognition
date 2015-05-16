package ua.kiev.icyb.bio.test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Медленно выполнимые тесты (больше 10 секунд на тест).
 */
@RunWith(Categories.class)
@IncludeCategory(SlowTest.class)
@SuiteClasses({
	SetTests.class,
	FilterTests.class,
	DistributionTests.class,
	MixtureTests.class,
	TreeTests.class,
	ToolTests.class,
	IOTests.class,
	AlgorithmTests.class
})
public class SlowTestSuite {
	/* Пусто. */
}
