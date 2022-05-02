namespace Data;

using System.Reflection;
using System.Text;

public class SquareField
{

    public int Id { get; set; }

    public List<Coord> edges { get; set; }

    public List<int> fogNodeIDs { get; set;}

    public List<Coord> locations { get; set; }

    public override string? ToString()
    {
        Type objType = this.GetType();
        PropertyInfo[] propertyInfoList = objType.GetProperties();
        StringBuilder result = new StringBuilder();
        foreach (PropertyInfo propertyInfo in propertyInfoList)
            result.AppendFormat("{0}={1} ", propertyInfo.Name, propertyInfo.GetValue(this));

        return result.ToString();
    }
}