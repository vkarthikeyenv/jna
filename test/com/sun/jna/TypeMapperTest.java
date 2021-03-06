/* Copyright (c) 2007 Wayne Meissner, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package com.sun.jna;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

//@SuppressWarnings("unused")
public class TypeMapperTest extends TestCase {

    private static final String UNICODE = "[\0444]";

    public static interface TestLibrary extends Library {
        int returnInt32Argument(boolean b);
        int returnInt32Argument(String s);
        int returnInt32Argument(Number n);
        WString returnWStringArgument(String s);
        String returnWStringArgument(WString s);
    }

    public void testBooleanToIntArgumentConversion() {
        final int MAGIC = 0xABEDCF23;
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addToNativeConverter(Boolean.class, new ToNativeConverter() {
            @Override
            public Object toNative(Object arg, ToNativeContext ctx) {
                return Integer.valueOf(Boolean.TRUE.equals(arg) ? MAGIC : 0);
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        });
        TestLibrary lib = Native.loadLibrary("testlib", TestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert Boolean argument to Int", MAGIC, lib.returnInt32Argument(true));
    }
    public void testStringToIntArgumentConversion() {
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addToNativeConverter(String.class, new ToNativeConverter() {
            @Override
            public Object toNative(Object arg, ToNativeContext ctx) {
                return Integer.valueOf((String) arg, 16);
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        });
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = Native.loadLibrary("testlib", TestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert String argument to Int", MAGIC,
                     lib.returnInt32Argument(Integer.toHexString(MAGIC)));
    }
    public void testStringToWStringArgumentConversion() {
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addToNativeConverter(String.class, new ToNativeConverter() {
            @Override
            public Object toNative(Object arg, ToNativeContext ctx) {
                return new WString(arg.toString());
            }
            @Override
            public Class<?> nativeType() {
                return WString.class;
            }
        });
        final String MAGIC = "magic" + UNICODE;
        TestLibrary lib = Native.loadLibrary("testlib", TestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert String argument to WString", new WString(MAGIC),
                     lib.returnWStringArgument(MAGIC));
    }
    public void testCharSequenceToIntArgumentConversion() {
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addToNativeConverter(CharSequence.class, new ToNativeConverter() {
            @Override
            public Object toNative(Object arg, ToNativeContext ctx) {
                return Integer.valueOf(((CharSequence)arg).toString(), 16);
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        });
        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = Native.loadLibrary("testlib", TestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert String argument to Int", MAGIC, lib.returnInt32Argument(Integer.toHexString(MAGIC)));
    }
    public void testNumberToIntArgumentConversion() {
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addToNativeConverter(Double.class, new ToNativeConverter() {
            @Override
            public Object toNative(Object arg, ToNativeContext ctx) {
                return Integer.valueOf(((Double)arg).intValue());
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        });

        final int MAGIC = 0x7BEDCF23;
        TestLibrary lib = Native.loadLibrary("testlib", TestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert Double argument to Int", MAGIC,
                     lib.returnInt32Argument(Double.valueOf(MAGIC)));
    }
    public void testWStringToStringResultConversion() throws Exception {
        final String MAGIC = "magic" + UNICODE;
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addFromNativeConverter(String.class, new FromNativeConverter() {
            @Override
            public Object fromNative(Object value, FromNativeContext ctx) {
                if (value == null) {
                    return null;
                }
                return value.toString();
            }
            @Override
            public Class<?> nativeType() {
                return WString.class;
            }
        });
        TestLibrary lib = Native.loadLibrary("testlib", TestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert WString result to String", MAGIC, lib.returnWStringArgument(new WString(MAGIC)));
    }

    public static interface BooleanTestLibrary extends Library {
        boolean returnInt32Argument(boolean b);
    }
    public void testIntegerToBooleanResultConversion() throws Exception {
        final int MAGIC = 0xABEDCF23;
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        mapper.addToNativeConverter(Boolean.class, new ToNativeConverter() {
            @Override
            public Object toNative(Object value, ToNativeContext ctx) {
                return Integer.valueOf(Boolean.TRUE.equals(value) ? MAGIC : 0);
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        });
        mapper.addFromNativeConverter(Boolean.class, new FromNativeConverter() {
            @Override
            public Object fromNative(Object value, FromNativeContext context) {
                return Boolean.valueOf(((Integer) value).intValue() == MAGIC);
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        });
        BooleanTestLibrary lib = Native.loadLibrary("testlib", BooleanTestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Failed to convert integer return to boolean TRUE", true, lib.returnInt32Argument(true));
        assertEquals("Failed to convert integer return to boolean FALSE", false, lib.returnInt32Argument(false));
    }

    public static interface StructureTestLibrary extends Library {
        public static class TestStructure extends Structure {
            public TestStructure(TypeMapper mapper) {
                super(mapper);
            }
            public boolean data;
            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList("data");
            }
        }
    }
    public void testStructureConversion() throws Exception {
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        TypeConverter converter = new TypeConverter() {
            @Override
            public Object toNative(Object value, ToNativeContext ctx) {
                return Integer.valueOf(Boolean.TRUE.equals(value) ? 1 : 0);
            }
            @Override
            public Object fromNative(Object value, FromNativeContext context) {
                return Boolean.valueOf(((Integer)value).intValue() == 1);
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        };
        mapper.addTypeConverter(Boolean.class, converter);
		Native.loadLibrary("testlib", StructureTestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        StructureTestLibrary.TestStructure s = new StructureTestLibrary.TestStructure(mapper);
        assertEquals("Wrong native size", 4, s.size());

        s.data = true;
        s.write();
        assertEquals("Wrong value written", 1, s.getPointer().getInt(0));

        s.getPointer().setInt(0, 0);
        s.read();
        assertFalse("Wrong value read", s.data);
    }

    public static enum Enumeration {
        STATUS_0(0), STATUS_1(1), STATUS_ERROR(-1);
        private final int code;
        Enumeration(int code) { this.code = code; }
        public int getCode() { return code; }
        public static Enumeration fromCode(int code) {
            switch(code) {
            case 0: return STATUS_0;
            case 1: return STATUS_1;
            default: return STATUS_ERROR;
            }
        }
    }
    public static interface EnumerationTestLibrary extends Library {
        Enumeration returnInt32Argument(Enumeration arg);
    }
    public void testEnumConversion() throws Exception {
        DefaultTypeMapper mapper = new DefaultTypeMapper();
        TypeConverter converter = new TypeConverter() {
            @Override
            public Object toNative(Object value, ToNativeContext ctx) {
                return Integer.valueOf(((Enumeration)value).getCode());
            }
            @Override
            public Object fromNative(Object value, FromNativeContext context) {
                return Enumeration.fromCode(((Integer)value).intValue());
            }
            @Override
            public Class<?> nativeType() {
                return Integer.class;
            }
        };
        mapper.addTypeConverter(Enumeration.class, converter);
        EnumerationTestLibrary lib = Native.loadLibrary("testlib", EnumerationTestLibrary.class, Collections.singletonMap(Library.OPTION_TYPE_MAPPER, mapper));
        assertEquals("Enumeration improperly converted", Enumeration.STATUS_1, lib.returnInt32Argument(Enumeration.STATUS_1));
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TypeMapperTest.class);
    }
}
