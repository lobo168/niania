

package mgr.niania;

final class FrameAverage
{
    private final int mNumValues;
    private final long[] mValues;
    private int mEnd = 0;
    private int mLength = 0;
    private long mSum = 0L;

    FrameAverage(final int numValues)
    {
        super();
        mNumValues = numValues;
        mValues = new long[numValues];
    }

    void update(final long value)
    {
        mSum -= mValues[mEnd];
        mValues[mEnd] = value;
        mEnd = (mEnd + 1) % mNumValues;
        if (mLength < mNumValues)
        {
            mLength++;
        } // if
        mSum += value;
    } 

    double getAverage()
    {
        return mSum / (double) mLength;
    }

}

