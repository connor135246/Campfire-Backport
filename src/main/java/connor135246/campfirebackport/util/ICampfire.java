package connor135246.campfirebackport.util;

public interface ICampfire
{

    public boolean isLit();

    /**
     * @return the typeIndex used for certain configuration purposes, like getting a particular index from an array
     */
    public default int getActingTypeIndex()
    {
        return EnumCampfireType.index(getTypeIndex());
    }

    /**
     * @return the true typeIndex
     */
    public int getTypeIndex();

}
