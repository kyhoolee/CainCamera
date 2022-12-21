package com.cgfay.video.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * type of effect
 */
public class EffectType implements Parcelable {

    private EffectMimeType mimeType;
    // Types of special effects, filter effects, transition effects and time effects

    private String mThumb;  // Thumbnail path
    private String mName;   // Effect name
    private int id;
    // The special effect id is useless for the time being.
    // It was originally an id reserved for dynamic processing.

    public EffectType(EffectMimeType mimeType, String name, int id, String thumbPath) {
        this.mimeType = mimeType;
        this.mName = name;
        this.id = id;
        this.mThumb = thumbPath;
    }

    private EffectType(Parcel in) {
        id = in.readInt();
        mName = in.readString();
        mThumb = in.readString();
        mimeType = in.readParcelable(EffectMimeType.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(mName);
        dest.writeString(mThumb);
        dest.writeParcelable(mimeType, flags);
    }

    @Override
    public String toString() {
        return "(id=" + id + ", name=" + mName
                + ", mThumb=" + mThumb + ",mimeType=" + mimeType + ")";
    }

    public EffectMimeType getMimeType() {
        return mimeType;
    }

    public String getThumb() {
        return mThumb;
    }

    public String getName() {
        return mName;
    }

    public int getId() {
        return id;
    }

    public static final Creator<EffectType> CREATOR = new Creator<EffectType>() {
        @Override
        public EffectType createFromParcel(Parcel source) {
            return new EffectType(source);
        }

        @Override
        public EffectType[] newArray(int size) {
            return new EffectType[size];
        }
    };
}
