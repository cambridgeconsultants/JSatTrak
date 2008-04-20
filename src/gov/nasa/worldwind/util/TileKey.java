/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Angle;

/**
 * @author tag
 * @version $Id: TileKey.java 2471 2007-07-31 21:50:57Z tgaskins $
 */
public class TileKey implements Comparable<TileKey>
{
    private final int level;
    private final int row;
    private final int col;
    private final String cacheName;
    private final int hash;

    /**
     * @param level
     * @param row
     * @param col
     * @param cacheName
     * @throws IllegalArgumentException if <code>level</code>, <code>row</code> or <code>column</code> is negative or if
     *                                  <code>cacheName</code> is null or empty
     */
    public TileKey(int level, int row, int col, String cacheName)
    {
        if (level < 0)
        {
            String msg = Logging.getMessage("TileKey.levelIsLessThanZero");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (row < 0)
        {
            String msg = Logging.getMessage("generic.RowIndexOutOfRange", row);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (col < 0)
        {
            String msg = Logging.getMessage("generic.ColumnIndexOutOfRange", col);
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (cacheName == null || cacheName.length() < 1)
        {
            String msg = Logging.getMessage("TileKey.cacheNameIsNullOrEmpty");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.level = level;
        this.row = row;
        this.col = col;
        this.cacheName = cacheName;
        this.hash = this.computeHash();
    }

    /**
     * @param latitude
     * @param longitude
     * @param level
     * @throws IllegalArgumentException if any parameter is null
     */
    public TileKey(Angle latitude, Angle longitude, Level level)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        if (level == null)
        {
            String msg = Logging.getMessage("nullValue.LevelIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.level = level.getLevelNumber();
        this.row = Tile.computeRow(level.getTileDelta().getLatitude(), latitude);
        this.col = Tile.computeColumn(level.getTileDelta().getLongitude(), longitude);
        this.cacheName = level.getCacheName();
        this.hash = this.computeHash();
    }

    /**
     * @param tile
     * @throws IllegalArgumentException if <code>tile</code> is null
     */
    public TileKey(Tile tile)
    {
        if (tile == null)
        {
            String msg = Logging.getMessage("nullValue.TileIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }
        this.level = tile.getLevelNumber();
        this.row = tile.getRow();
        this.col = tile.getColumn();
        this.cacheName = tile.getCacheName();
        this.hash = this.computeHash();
    }

    public int getLevelNumber()
    {
        return level;
    }

    public int getRow()
    {
        return row;
    }

    public int getColumn()
    {
        return col;
    }

    public String getCacheName()
    {
        return cacheName;
    }

    private int computeHash()
    {
        int result;
        result = this.level;
        result = 29 * result + this.row;
        result = 29 * result + this.col;
        result = 29 * result + (this.cacheName != null ? this.cacheName.hashCode() : 0);
        return result;
    }

    /**
     * @param key
     * @return
     * @throws IllegalArgumentException if <code>key</code> is null
     */
    public final int compareTo(TileKey key)
    {
        if (key == null)
        {
            String msg = Logging.getMessage("nullValue.KeyIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        // No need to compare Sectors because they are redundant with row and column
        if (key.level == this.level && key.row == this.row && key.col == this.col)
            return 0;

        if (this.level < key.level) // Lower-res levels compare lower than higher-res
            return -1;
        if (this.level > key.level)
            return 1;

        if (this.row < key.row)
            return -1;
        if (this.row > key.row)
            return 1;

        if (this.col < key.col)
            return -1;

        return 1; // tile.col must be > this.col because equality was tested above
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TileKey tileKey = (TileKey) o;

        if (this.col != tileKey.col)
            return false;
        if (this.level != tileKey.level)
            return false;
        //noinspection SimplifiableIfStatement
        if (this.row != tileKey.row)
            return false;

        return !(this.cacheName != null ? !this.cacheName.equals(tileKey.cacheName) : tileKey.cacheName != null);
    }

    @Override
    public int hashCode()
    {
        return this.hash;
    }

    @Override
    public String toString()
    {
        return this.cacheName + "/" + this.level + "/" + this.row + "/" + col;
    }
}
