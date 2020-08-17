package org.beaglebasic.domain;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.beaglebasic.error.BeagleBasicInternalError;
import org.beaglebasic.error.BeagleBasicRuntimeError;
import org.beaglebasic.runtime.Formatter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.beaglebasic.error.BeagleBasicRuntimeError.ErrorCode.ARRAY_INDEX_OUT_OF_BOUNDS;
import static org.beaglebasic.error.BeagleBasicRuntimeError.ErrorCode.ILLEGAL_FUNCTION_PARAM;

public class STObjects {

    public enum BeagleBasicDataType {

        INT32('%') {
            @Override
            public STVariable createVariableEntry(Variable variable) {
                if (variable.isArray()) {
                    return new STVariable(STKind.VARIABLE, new STInt32ArrayValue(), variable);
                } if (variable.isUDF()) {
                    return new STUDF(STKind.VARIABLE, new STInt32ScalarValue(), variable);
                } else {
                    return new STVariable(STKind.VARIABLE, new STInt32ScalarValue(), variable);
                }
            }

            @Override
            public STTmp createTmpEntry() {
                return new STTmp(STKind.TMP, new STInt32ScalarValue());
            }
        },
        INT64('@') {
            @Override
            public STVariable createVariableEntry(Variable variable) {
                if (variable.isArray()) {
                    return new STVariable(STKind.VARIABLE, new STInt64ArrayValue(), variable);
                } if (variable.isUDF()) {
                    return new STUDF(STKind.VARIABLE, new STInt64ScalarValue(), variable);
                } else {
                    return new STVariable(STKind.VARIABLE, new STInt64ScalarValue(), variable);
                }
            }

            @Override
            public STTmp createTmpEntry() {
                return new STTmp(STKind.TMP, new STInt64ScalarValue());
            }
        },
        FLOAT('!') {
            @Override
            public STVariable createVariableEntry(Variable variable) {
                if (variable.isArray()) {
                    return new STVariable(STKind.VARIABLE, new STFloat32ArrayValue(), variable);
                } if (variable.isUDF()) {
                    return new STUDF(STKind.VARIABLE, new STFloat32ScalarValue(), variable);
                } else {
                    return new STVariable(STKind.VARIABLE, new STFloat32ScalarValue(), variable);
                }
            }

            @Override
            public STTmp createTmpEntry() {
                return new STTmp(STKind.TMP, new STFloat32ScalarValue());
            }
        },
        DOUBLE('#') {
            @Override
            public STVariable createVariableEntry(Variable variable) {
                if (variable.isArray()) {
                    return new STVariable(STKind.VARIABLE, new STFloat64ArrayValue(), variable);
                } if (variable.isUDF()) {
                    return new STUDF(STKind.VARIABLE, new STFloat64ScalarValue(), variable);
                } else {
                    return new STVariable(STKind.VARIABLE, new STFloat64ScalarValue(), variable);
                }
            }

            @Override
            public STTmp createTmpEntry() {
                return new STTmp(STKind.TMP, new STFloat64ScalarValue());
            }
        },
        STRING('$') {
            @Override
            public STVariable createVariableEntry(Variable variable) {
                if (variable.isArray()) {
                    return new STVariable(STKind.VARIABLE, new STStringArrayValue(), variable);
                } if (variable.isUDF()) {
                    return new STUDF(STKind.VARIABLE, new STStringScalarValue(), variable);
                } else {
                    String varname = variable.getVariableName().getVarname();
                    if (varname.equalsIgnoreCase("date")) {
                        return new STVariable(STKind.VARIABLE, new STStringScalarDateValue(), variable);
                    } else if (varname.equalsIgnoreCase("time")) {
                        return new STVariable(STKind.VARIABLE, new STStringScalarTimeValue(), variable);
                    } else {
                        return new STVariable(STKind.VARIABLE, new STStringScalarValue(), variable);
                    }
                }
            }

            @Override
            public STTmp createTmpEntry() {
                return new STTmp(STKind.TMP, new STStringScalarValue());
            }
        };

        private static final Int2ObjectMap<BeagleBasicDataType> mapping;

        static {
            mapping = new Int2ObjectOpenHashMap<>();
            for (BeagleBasicDataType value : BeagleBasicDataType.values()) {
                mapping.put(value.repr, value);
            }
        }

        public final char repr;

        BeagleBasicDataType(char repr) {
            this.repr = repr;
        }

        public abstract STVariable createVariableEntry(Variable variable);

        public abstract STTmp createTmpEntry();

        public static BeagleBasicDataType lookup(String repr) {
            if (repr == null || repr.length() != 1) {
                throw new BeagleBasicInternalError(
                        "Variable suffix: '" + repr + "' is null or length != 1"
                );
            }
            var dataType = mapping.get(repr.charAt(0));
            if (dataType == null) {
                throw new BeagleBasicInternalError(
                        "Variable suffix '" + repr + "' is invalid"
                );
            }
            return dataType;
        }
    }

    public enum STKind {
        TMP,
        VARIABLE,
        LABEL
    }

    public interface STEntry {
        STKind getKind();
        STValue getValue();
    }

    public static abstract class AbstractSTEntry implements STEntry {
        private final STKind kind;
        private final STValue value;

        public AbstractSTEntry(STKind kind, STValue value) {
            this.kind = kind;
            this.value = value;
        }

        @Override
        public STKind getKind() {
            return kind;
        }

        @Override
        public STValue getValue() {
            return value;
        }
    }

    public static class STVariable extends AbstractSTEntry {
        private final Variable variable;

        public STVariable(STKind kind, STValue value, Variable variable) {
            super(kind, value);
            this.variable = variable;
        }

        public Variable getVariable() {
            return variable;
        }
    }

    public static final class STUDF extends STVariable {

        private final IntList paramIds;

        public STUDF(STKind kind, STValue value, Variable variable) {
            super(kind, value, variable);
            this.paramIds = new IntArrayList();
        }

        public void declareParam(int paramId) {
            paramIds.add(paramId);
        }

        public int getNumDeclaredParams() {
            return paramIds.size();
        }

        public int getDeclaraedParam(int i) {
            return paramIds.get(i);
        }
    }

    public static final class STTmp extends AbstractSTEntry {
        public STTmp(STKind kind, STValue value) {
            super(kind, value);
        }
    }

    public static final class STLabel extends AbstractSTEntry {
        public STLabel() {
            super(STKind.LABEL, new STInt32ScalarValue());
        }
    }

    public interface STValue {
        BeagleBasicDataType getDataType();
        String printFormat();
        String writeFormat();
        void assign(STValue entry);
        int getInt32();
        long getInt64();
        float getFloat32();
        double getFloat64();
        int getRoundedInt32();
        long getRoundedInt64();
        String getString();
        void setInt32(int value);
        void setInt64(long value);
        void setFloat32(float value);
        void setFloat64(double value);
        void setString(String value);
        default int getFieldLength() {
            return 0;
        }
        default void setFieldLength(int fieldLength) {}
        default void setArrayDimensions(IntList dims) {}
        default IntList getArrayDimensions() {
            return new IntArrayList();
        }
        default void setArrayIndex(int dim, int index) {}
        default void resetArrayIndex() {}
        default int getArrayIndex1D() {
            return 0;
        }
    }

    private static final class STInt32ScalarValue implements STValue {

        private int value;

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.INT32;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatInt32(value);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatInt32(value);
        }

        @Override
        public void assign(STValue entry) {
            this.value = entry.getInt32();
        }

        @Override
        public int getInt32() {
            return value;
        }

        @Override
        public long getInt64() {
            return value;
        }

        @Override
        public float getFloat32() {
            return value;
        }

        @Override
        public double getFloat64() {
            return value;
        }

        @Override
        public int getRoundedInt32() {
            return value;
        }

        @Override
        public long getRoundedInt64() {
            return value;
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value = value;
        }

        @Override
        public void setInt64(long value) {
            this.value = (int) value;
        }

        @Override
        public void setFloat32(float value) {
            this.value = (int) value;
        }

        @Override
        public void setFloat64(double value) {
            this.value = (int) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to int32: '" + value + "'");
        }
    }

    private static final class STInt64ScalarValue implements STValue {

        private long value;

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.INT64;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatInt64(value);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatInt64(value);
        }

        @Override
        public void assign(STValue entry) {
            this.value = entry.getInt64();
        }

        @Override
        public int getInt32() {
            return (int) value;
        }

        @Override
        public long getInt64() {
            return value;
        }

        @Override
        public float getFloat32() {
            return value;
        }

        @Override
        public double getFloat64() {
            return value;
        }

        @Override
        public int getRoundedInt32() {
            return (int) value;
        }

        @Override
        public long getRoundedInt64() {
            return value;
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast int64 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value = value;
        }

        @Override
        public void setInt64(long value) {
            this.value = value;
        }

        @Override
        public void setFloat32(float value) {
            this.value = (long) value;
        }

        @Override
        public void setFloat64(double value) {
            this.value = (long) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to int64: '" + value + "'");
        }
    }

    private static final class STFloat32ScalarValue implements STValue {

        private float value;

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.FLOAT;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatFloat32(value);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatFloat32(value);
        }

        @Override
        public void assign(STValue entry) {
            this.value = entry.getFloat32();
        }

        @Override
        public int getInt32() {
            return (int) value;
        }

        @Override
        public long getInt64() {
            return (long) value;
        }

        @Override
        public float getFloat32() {
            return value;
        }

        @Override
        public double getFloat64() {
            return value;
        }

        @Override
        public int getRoundedInt32() {
            return Math.round(value);
        }

        @Override
        public long getRoundedInt64() {
            return Math.round(value);
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast float32 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value = value;
        }

        @Override
        public void setInt64(long value) {
            this.value = value;
        }

        @Override
        public void setFloat32(float value) {
            this.value = value;
        }

        @Override
        public void setFloat64(double value) {
            this.value = (float) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to float32: '" + value + "'");
        }
    }

    private static final class STFloat64ScalarValue implements STValue {

        private double value;

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.DOUBLE;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatFloat64(value);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatFloat64(value);
        }

        @Override
        public void assign(STValue entry) {
            this.value = entry.getFloat64();
        }

        @Override
        public int getInt32() {
            return (int) value;
        }

        @Override
        public long getInt64() {
            return (long) value;
        }

        @Override
        public float getFloat32() {
            return (float) value;
        }

        @Override
        public double getFloat64() {
            return value;
        }

        @Override
        public int getRoundedInt32() {
            return (int) Math.round(value);
        }

        @Override
        public long getRoundedInt64() {
            return Math.round(value);
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast float64 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value = value;
        }

        @Override
        public void setInt64(long value) {
            this.value = value;
        }

        @Override
        public void setFloat32(float value) {
            this.value = value;
        }

        @Override
        public void setFloat64(double value) {
            this.value = value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to float64: '" + value + "'");
        }
    }

    private static final class STStringScalarValue implements STValue {

        private int fieldLength;
        private String value = "";

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.STRING;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatString(value);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatString(value);
        }

        @Override
        public void assign(STValue entry) {
            this.value = entry.getString();
        }

        @Override
        public int getInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public float getFloat32() {
            throw new BeagleBasicInternalError("Can't cast String to float32");
        }

        @Override
        public double getFloat64() {
            throw new BeagleBasicInternalError("Can't cast String to float64");
        }

        @Override
        public int getRoundedInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getRoundedInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public String getString() {
            return value;
        }

        @Override
        public void setInt32(int value) {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt64(long value) {
            throw new BeagleBasicInternalError("Can't cast int64 to String");
        }

        @Override
        public void setFloat32(float value) {
            throw new BeagleBasicInternalError("Can't cast float32 to String");
        }

        @Override
        public void setFloat64(double value) {
            throw new BeagleBasicInternalError("Can't cast float64 to String");
        }

        @Override
        public void setString(String value) {
            this.value = value;
        }

        @Override
        public int getFieldLength() {
            return fieldLength;
        }

        @Override
        public void setFieldLength(int fieldLength) {
            this.fieldLength = fieldLength;
        }
    }

    private static final class STStringScalarTimeValue implements STValue {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
        private LocalTime time;

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.STRING;
        }

        @Override
        public String printFormat() {
            return getString();
        }

        @Override
        public String writeFormat() {
            return getString();
        }

        @Override
        public void assign(STValue entry) {
            setString(entry.getString());
        }

        @Override
        public int getInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public float getFloat32() {
            throw new BeagleBasicInternalError("Can't cast String to float32");
        }

        @Override
        public double getFloat64() {
            throw new BeagleBasicInternalError("Can't cast String to float64");
        }

        @Override
        public int getRoundedInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getRoundedInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public String getString() {
            return formatLocalTime(time != null ? time : LocalTime.now());
        }

        private String formatLocalTime(LocalTime time) {
            return time.format(FORMATTER);
        }

        @Override
        public void setInt32(int value) {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt64(long value) {
            throw new BeagleBasicInternalError("Can't cast int64 to String");
        }

        @Override
        public void setFloat32(float value) {
            throw new BeagleBasicInternalError("Can't cast float32 to String");
        }

        @Override
        public void setFloat64(double value) {
            throw new BeagleBasicInternalError("Can't cast float64 to String");
        }

        @Override
        public void setString(String value) {
            this.time = LocalTime.parse(value, FORMATTER);
        }

        @Override
        public int getFieldLength() {
            return 0;
        }

        @Override
        public void setFieldLength(int fieldLength) {
            throw new BeagleBasicRuntimeError(
                    ILLEGAL_FUNCTION_PARAM,
                    "TIME$ cannot be used for setting field length!"
            );
        }
    }

    private static final class STStringScalarDateValue implements STValue {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
        private LocalDate date;

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.STRING;
        }

        @Override
        public String printFormat() {
            return getString();
        }

        @Override
        public String writeFormat() {
            return getString();
        }

        @Override
        public void assign(STValue entry) {
            setString(entry.getString());
        }

        @Override
        public int getInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public float getFloat32() {
            throw new BeagleBasicInternalError("Can't cast String to float32");
        }

        @Override
        public double getFloat64() {
            throw new BeagleBasicInternalError("Can't cast String to float64");
        }

        @Override
        public int getRoundedInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getRoundedInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public String getString() {
            return formatLocalDate(date != null ? date : LocalDate.now());
        }

        private String formatLocalDate(LocalDate date) {
            return date.format(FORMATTER);
        }

        @Override
        public void setInt32(int value) {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt64(long value) {
            throw new BeagleBasicInternalError("Can't cast int64 to String");
        }

        @Override
        public void setFloat32(float value) {
            throw new BeagleBasicInternalError("Can't cast float32 to String");
        }

        @Override
        public void setFloat64(double value) {
            throw new BeagleBasicInternalError("Can't cast float64 to String");
        }

        @Override
        public void setString(String value) {
            this.date = LocalDate.parse(value, FORMATTER);
        }

        @Override
        public int getFieldLength() {
            return 0;
        }

        @Override
        public void setFieldLength(int fieldLength) {
            throw new BeagleBasicRuntimeError(
                    ILLEGAL_FUNCTION_PARAM,
                    "DATE$ cannot be used for setting field length!"
            );
        }
    }

    private static abstract class AbstractSTArrayValue implements STValue {

        private IntList dimensions;
        private int totalLength;
        private int index1d;

        protected int getTotalLength() {
            return totalLength;
        }

        @Override
        public void setArrayDimensions(IntList dims) {
            this.dimensions = new IntArrayList(dims);
            int totalLen = 1;
            for (int i = 0; i < dims.size(); i++) {
                totalLen *= dimensions.getInt(i);
            }
            totalLength = totalLen;
        }

        @Override
        public IntList getArrayDimensions() {
            return new IntArrayList(dimensions);
        }

        @Override
        public void resetArrayIndex() {
            this.index1d = 0;
        }

        @Override
        public void setArrayIndex(int dim, int index) {
            if (dim < 0 || dim >= dimensions.size()) {
                throw new BeagleBasicRuntimeError(
                        ARRAY_INDEX_OUT_OF_BOUNDS,
                        "Dimension index " + dim + " is out of range, #dims=" + dimensions.size()
                );
            }
            if (index < 0 || index >= dimensions.getInt(dim)) {
                throw new BeagleBasicRuntimeError(
                        ARRAY_INDEX_OUT_OF_BOUNDS,
                        "Index " + index + " is out of range for dimension["
                                + dim + "]=" + dimensions.getInt(dim)
                );
            }
            this.index1d = this.index1d * (dim > 0 ? dimensions.getInt(dim - 1) : 0) + index;
        }

        @Override
        public int getArrayIndex1D() {
            return index1d;
        }
    }

    private static final class STInt32ArrayValue extends AbstractSTArrayValue {

        private int[] value;

        int[] getValue() {
            return value;
        }

        @Override
        public void setArrayDimensions(IntList dims) {
            super.setArrayDimensions(dims);
            this.value = new int[getTotalLength()];
        }

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.INT32;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatInt32(value[getArrayIndex1D()]);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatInt32(value[getArrayIndex1D()]);
        }

        @Override
        public void assign(STValue entry) {
            value[getArrayIndex1D()] = entry.getInt32();
        }

        @Override
        public int getInt32() {
            return value[getArrayIndex1D()];
        }

        @Override
        public long getInt64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public float getFloat32() {
            return value[getArrayIndex1D()];
        }

        @Override
        public double getFloat64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public int getRoundedInt32() {
            return value[getArrayIndex1D()];
        }

        @Override
        public long getRoundedInt64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value[getArrayIndex1D()] = value;
        }

        @Override
        public void setInt64(long value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat32(float value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat64(double value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to int32: '" + value + "'");
        }
    }

    private static final class STInt64ArrayValue extends AbstractSTArrayValue {

        private long[] value;

        long[] getValue() {
            return value;
        }

        @Override
        public void setArrayDimensions(IntList dims) {
            super.setArrayDimensions(dims);
            this.value = new long[getTotalLength()];
        }

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.INT64;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatInt64(value[getArrayIndex1D()]);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatInt64(value[getArrayIndex1D()]);
        }

        @Override
        public void assign(STValue entry) {
            value[getArrayIndex1D()] = entry.getInt64();
        }

        @Override
        public int getInt32() {
            return (int) value[getArrayIndex1D()];
        }

        @Override
        public long getInt64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public float getFloat32() {
            return value[getArrayIndex1D()];
        }

        @Override
        public double getFloat64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public int getRoundedInt32() {
            return (int) value[getArrayIndex1D()];
        }

        @Override
        public long getRoundedInt64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value[getArrayIndex1D()] = value;
        }

        @Override
        public void setInt64(long value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat32(float value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat64(double value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to int32: '" + value + "'");
        }
    }

    private static final class STFloat32ArrayValue extends AbstractSTArrayValue {

        private float[] value;

        float[] getValue() {
            return value;
        }

        @Override
        public void setArrayDimensions(IntList dims) {
            super.setArrayDimensions(dims);
            this.value = new float[getTotalLength()];
        }

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.FLOAT;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatFloat32(value[getArrayIndex1D()]);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatFloat32(value[getArrayIndex1D()]);
        }

        @Override
        public void assign(STValue entry) {
            value[getArrayIndex1D()] = entry.getFloat32();
        }

        @Override
        public int getInt32() {
            return (int) value[getArrayIndex1D()];
        }

        @Override
        public long getInt64() {
            return (long) value[getArrayIndex1D()];
        }

        @Override
        public float getFloat32() {
            return value[getArrayIndex1D()];
        }

        @Override
        public double getFloat64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public int getRoundedInt32() {
            return Math.round(value[getArrayIndex1D()]);
        }

        @Override
        public long getRoundedInt64() {
            return Math.round(value[getArrayIndex1D()]);
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value[getArrayIndex1D()] = value;
        }

        @Override
        public void setInt64(long value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat32(float value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat64(double value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to int32: '" + value + "'");
        }
    }

    private static final class STFloat64ArrayValue extends AbstractSTArrayValue {

        private double[] value;

        double[] getValue() {
            return value;
        }

        @Override
        public void setArrayDimensions(IntList dims) {
            super.setArrayDimensions(dims);
            this.value = new double[getTotalLength()];
        }

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.DOUBLE;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatFloat64(value[getArrayIndex1D()]);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatFloat64(value[getArrayIndex1D()]);
        }

        @Override
        public void assign(STValue entry) {
            value[getArrayIndex1D()] = entry.getFloat64();
        }

        @Override
        public int getInt32() {
            return (int) value[getArrayIndex1D()];
        }

        @Override
        public long getInt64() {
            return (long) value[getArrayIndex1D()];
        }

        @Override
        public float getFloat32() {
            return (float) value[getArrayIndex1D()];
        }

        @Override
        public double getFloat64() {
            return value[getArrayIndex1D()];
        }

        @Override
        public int getRoundedInt32() {
            return (int) Math.round(value[getArrayIndex1D()]);
        }

        @Override
        public long getRoundedInt64() {
            return Math.round(value[getArrayIndex1D()]);
        }

        @Override
        public String getString() {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt32(int value) {
            this.value[getArrayIndex1D()] = value;
        }

        @Override
        public void setInt64(long value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat32(float value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setFloat64(double value) {
            this.value[getArrayIndex1D()] = (int) value;
        }

        @Override
        public void setString(String value) {
            throw new BeagleBasicInternalError("Can't cast String to int32: '" + value + "'");
        }
    }

    private static final class STStringArrayValue extends AbstractSTArrayValue {

        private String[] value;

        String[] getValue() {
            return value;
        }

        @Override
        public void setArrayDimensions(IntList dims) {
            super.setArrayDimensions(dims);
            this.value = new String[getTotalLength()];
            Arrays.fill(value, 0, value.length, "");
        }

        @Override
        public BeagleBasicDataType getDataType() {
            return BeagleBasicDataType.INT32;
        }

        @Override
        public String printFormat() {
            return Formatter.printFormatString(value[getArrayIndex1D()]);
        }

        @Override
        public String writeFormat() {
            return Formatter.writeFormatString(value[getArrayIndex1D()]);
        }

        @Override
        public void assign(STValue entry) {
            value[getArrayIndex1D()] = entry.getString();
        }

        @Override
        public int getInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public float getFloat32() {
            throw new BeagleBasicInternalError("Can't cast String to float32");
        }

        @Override
        public double getFloat64() {
            throw new BeagleBasicInternalError("Can't cast String to float64");
        }

        @Override
        public int getRoundedInt32() {
            throw new BeagleBasicInternalError("Can't cast String to int32");
        }

        @Override
        public long getRoundedInt64() {
            throw new BeagleBasicInternalError("Can't cast String to int64");
        }

        @Override
        public String getString() {
            return value[getArrayIndex1D()];
        }

        @Override
        public void setInt32(int value) {
            throw new BeagleBasicInternalError("Can't cast int32 to String");
        }

        @Override
        public void setInt64(long value) {
            throw new BeagleBasicInternalError("Can't cast int64 to String");
        }

        @Override
        public void setFloat32(float value) {
            throw new BeagleBasicInternalError("Can't cast float32 to String");
        }

        @Override
        public void setFloat64(double value) {
            throw new BeagleBasicInternalError("Can't cast float64 to String");
        }

        @Override
        public void setString(String value) {
            this.value[getArrayIndex1D()] = value;
        }
    }
}