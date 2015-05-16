package ua.kiev.icyb.bio.test;

import static org.junit.Assert.*;

import org.junit.Test;

import ua.kiev.icyb.bio.tools.Configuration;

/**
 * Тесты, связанные с инструментами.
 */
public class ToolTests {

	private static enum TestEnum {
		FIRST,
		SECOND;
	}
	
	private static class TestClass {
		public String strField;
		public int intField;
		public double dblField;
		
		public TestEnum enumField; 
	}
	
	/**
	 * Тестирует конструктор конфигурации.
	 */
	@Test
	public void testConfigurationInit() {
		Configuration conf = new Configuration("a=b", "c=d = e");
		assertEquals(2, conf.size());
		assertTrue(conf.containsKey("a"));
		assertTrue(conf.containsKey("c"));
		assertEquals("b", conf.get("a"));
		assertEquals("d = e", conf.get("c"));
	}
	
	/**
	 * Тестирует присвоение полей объекта при помощи конфигурации для полей "простых" типов.
	 */
	@Test
	public void testConfigurationAssign() {
		Configuration conf = new Configuration("strField=b", "intField=555", 
				"dblField=-1.34");
		TestClass obj = new TestClass();
		conf.setFields(obj);
		assertEquals("b", obj.strField);
		assertEquals(555, obj.intField);
		assertEquals(-1.34, obj.dblField, 1e-6);
	}
	
	/**
	 * Тестирует присвоение полей объекта при помощи конфигурации для поля-перечисления.
	 */
	@Test
	public void testConfigurationAssignEnum() {
		Configuration conf = new Configuration("enumField=first");
		TestClass obj = new TestClass();
		conf.setFields(obj);
		assertEquals(TestEnum.FIRST, obj.enumField);
		
		conf = new Configuration("enumField=SeConD");
		obj = new TestClass();
		conf.setFields(obj);
		assertEquals(TestEnum.SECOND, obj.enumField);
	}
}
