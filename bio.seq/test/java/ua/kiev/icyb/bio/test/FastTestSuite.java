package ua.kiev.icyb.bio.test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Быстро выполнимые тесты (меньше 5-10 секунд на тест).
 */
@RunWith(Categories.class)
@ExcludeCategory(SlowTest.class)
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
public class FastTestSuite {
	/* Пусто. */
}
