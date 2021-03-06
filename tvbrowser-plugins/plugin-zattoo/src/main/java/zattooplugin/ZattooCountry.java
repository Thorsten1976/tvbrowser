package zattooplugin;

public class ZattooCountry {
  private String mCode;
  private String mName;

  public ZattooCountry(final String code, final String name) {
    mCode = code;
    mName = name;
  }

  public String getCode() {
    return mCode;
  }

  public void setCode(final String code) {
    mCode = code;
  }

  public String getName() {
    return mName;
  }

  public void setName(final String name) {
    mName = name;
  }

  public String toString() {
    return mName;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ZattooCountry that = (ZattooCountry) o;

    if (mCode != null ? !mCode.equals(that.mCode) : that.mCode != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return mCode != null ? mCode.hashCode() : 0;
  }
}
