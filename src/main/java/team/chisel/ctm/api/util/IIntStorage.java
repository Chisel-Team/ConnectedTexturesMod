package team.chisel.ctm.api.util;

/**
 * Stores data in integers!
 */
public interface IIntStorage {

    /**
     * Get the size of this int storage, size is amount of bytes able to be stored
     */
    int getSize();

    /**
     * Get the int at the index
     */
    int getInt(int index);

    /**
     * Get the float at the index
     */
    float getFloat(int index);

    /**
     * Get the byte at the index
     */
    byte getByte(int index);

    /**
     * Get the short at the index
     */
    short getShort(int index);

    /**
     * Set the int to the specified value
     */
    void putInt(int index, int value);

    /**
     * Set the float to the specified value
     */
    void putFloat(int index, float value);

    /**
     * Set the byte to the specified value
     */
    void putByte(int index, byte value);

    /**
     * Set the short to the specified value
     */
    void putShort(int index, short value);

    /**
     * Creates an object view of this int storage that is limited to a specific area of the int storage
     *
     * It acts like a regular int storage with the length of the sub size
     */
    IIntStorage sub(int startIndex, int length);

    /**
     * Base implementation of IIntStorage
     */
    class BaseImpl implements IIntStorage {

        public static final int MASK_ONE = 0xff000000;

        public static final int MASK_TWO = 0x00ff0000;

        public static final int MASK_THREE = 0x0000ff00;

        public static final int MASK_FOUR = 0x000000ff;

        private int[] data;

        public BaseImpl(int[] data){
            this.data = data;
        }

        public int[] getData(){
            return this.data;
        }

        @Override
        public int getSize(){
            return this.data.length * 4;
        }

        @Override
        public int getInt(int index){
            int rem = index % 4;
            int num = index / 4;
            switch (rem){
                case 0 : return data[num];
                case 1 : return ((data[num] & (MASK_FOUR | MASK_THREE | MASK_TWO)) << 8) | ((data[num + 1] & MASK_ONE) >>> 24);
                case 2 : return ((data[num] & (MASK_FOUR | MASK_THREE)) << 16) | ((data[num + 1] & (MASK_ONE | MASK_TWO)) >>> 16);
                case 3 : return ((data[num] & MASK_FOUR) << 24) | ((data[num + 1] & (MASK_ONE | MASK_TWO | MASK_THREE)) >>> 8);
                default: return 0; //This
            }
        }

        @Override
        public float getFloat(int index){
            return Float.intBitsToFloat(getInt(index));
        }

        @Override
        public byte getByte(int index){
            int rem = index % 4;
            int num = index / 4;
            return (byte)(data[num] >> (8 * (3 - rem)) & 0xff);
        }

        @Override
        public short getShort(int index){
            int rem = index % 4;
            int num = index / 4;
            switch (rem){
                case 0 : return (short)((data[num] & (MASK_ONE | MASK_TWO)) >>> 16);
                case 1 : return (short)((data[num] & (MASK_TWO | MASK_THREE)) >>> 8);
                case 2 : return (short)(data[num] & (MASK_THREE | MASK_FOUR));
                case 3 : return (short)(((data[num] & MASK_FOUR) << 8) | ((data[num + 1] & MASK_ONE) >>> 24));
                default: return 0; //Impossible
            }
        }

        @Override
        public void putInt(int index, int val){
            int rem = index % 4;
            int num = index / 4;
            switch (rem){
                case 0 : data[num] = val;
                    break;
                case 1 : {
                    data[num] = (data[num] & MASK_ONE) | ((val & (MASK_ONE | MASK_TWO | MASK_THREE)) >>> 8);
                    data[num + 1] = (data[num + 1] & (MASK_TWO | MASK_THREE | MASK_FOUR)) | ((val & MASK_FOUR) << 24);
                    break;
                }
                case 2 : {
                    data[num] = (data[num] & (MASK_ONE | MASK_TWO)) | ((val & (MASK_ONE | MASK_TWO)) >>> 16);
                    data[num + 1] = (data[num + 1] & (MASK_THREE | MASK_FOUR)) | ((val & (MASK_THREE | MASK_FOUR)) << 16);
                    break;
                }
                case 3 : {
                    data[num] = (data[num] & (MASK_ONE | MASK_TWO | MASK_THREE)) | ((val & MASK_ONE) >>> 24);
                    data[num + 1] = (data[num + 1] & MASK_FOUR) | ((val & (MASK_TWO | MASK_THREE | MASK_FOUR)) << 8);
                    break;
                }
            }
        }

        @Override
        public void putFloat(int index, float val){
            putInt(index, Float.floatToRawIntBits(val));
        }

        @Override
        public void putByte(int index, byte value){
            int rem = index % 4;
            int num = index / 4;
            data[num] = (data[num] & ~(0xff << (8 * (3 - rem)))) | ((value & 0xff) << (8 * (3 - rem)));
        }

        @Override
        public void putShort(int index, short value){
            int rem = index % 4;
            int num = index / 4;
            //System.out.println("Num: " + num + " and rem " + rem);
            switch (rem){
                case 0 : data[num] = (data[num] & (MASK_THREE | MASK_FOUR)) | ((value & 0xff) << 16);
                    break;
                case 1 : data[num] = (data[num] & (MASK_ONE | MASK_FOUR)) | ((value & 0xff) << 8);
                    break;
                case 2 : this.data[num] = ((data[num] & (MASK_ONE | MASK_TWO)) | (value & 0xff));
                    break;
                case 3 : {
                    data[num] = (data[num] & ~0xff) | (value >>> 8);
                    data[num + 1] = (data[num + 1] & ~MASK_ONE) | ((value & 0xffff) << 24);
                    break;
                }
            }
        }

        @Override
        public IIntStorage sub(int startIndex, int length){
            return new SubImpl(this, startIndex, length);
        }
    }

    static IIntStorage create(int[] data){
        return new BaseImpl(data);
    }

    class SubImpl implements IIntStorage {

        private final IIntStorage parent;

        private final int startIndex;

        private final int length;

        public SubImpl(IIntStorage parent, int startIndex, int len){
            if (startIndex < 0 || len < 0){
                throw new RuntimeException("Start index or length cannot be less than zero!");
            }
            if (len == 0){
                throw new RuntimeException("Cannot have zero length!");
            }
            this.parent = parent;
            this.startIndex = startIndex;
            this.length = len;
        }

        private void checkIndex(int index, int dataLen){
            if (index < 0){
                throw new RuntimeException("Index cannot be negative!");
            }
            if ((index + dataLen) > this.length){
                throw new RuntimeException("Cannot access index length " + dataLen + " at index " + index);
            }
        }

        @Override
        public int getSize(){
            return this.length;
        }

        @Override
        public int getInt(int index){
            checkIndex(index, 4);
            return parent.getInt(this.startIndex + index);
        }

        @Override
        public float getFloat(int index){
            checkIndex(index, 4);
            return parent.getFloat(this.startIndex + index);
        }

        @Override
        public byte getByte(int index){
            checkIndex(index, 1);
            return parent.getByte(this.startIndex + index);
        }

        @Override
        public short getShort(int index){
            checkIndex(index, 2);
            return parent.getShort(this.startIndex + index);
        }

        @Override
        public void putInt(int index, int value){
            checkIndex(index, 4);
            parent.putInt(index, value);
        }

        @Override
        public void putFloat(int index, float value){
            checkIndex(index, 4);
            parent.putFloat(index, value);
        }

        @Override
        public void putByte(int index, byte value){
            checkIndex(index, 1);
            parent.putByte(index, value);
        }

        @Override
        public void putShort(int index, short value){
            checkIndex(index, 2);
            parent.putShort(index, value);
        }

        @Override
        public IIntStorage sub(int startIndex, int length){
            return new SubImpl(this, startIndex, length);
        }

    }
}
