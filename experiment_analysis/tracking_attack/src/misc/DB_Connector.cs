using System;
using System.Data;
using System.Data.SQLite;
using System.Globalization;
using System.Text;


public class DB_Connector
{

    //TODO
    private SQLiteConnection con;

    public DB_Connector()
    {
        con =  new SQLiteConnection("Data Source=" + Constants.DbPath);
    }

    public  Dictionary<int, List<Coord>> GetAllPaths() 
    {
        DataTable dt = new DataTable();

        Dictionary<int, List<Coord>> paths = new Dictionary<int, List<Coord>>();

        try
        {
            con.Open();

            var command = con.CreateCommand();
            command.CommandText = 
            @"
                SELECT *
                From paths
            ";
            var adapter = new SQLiteDataAdapter(command);
            adapter.Fill(dt);
        }
        catch(Exception e)
        {
            Console.WriteLine(e.StackTrace);
        }

        foreach(DataRow  row in dt.Rows)
        {   
            int pathId = Int32.Parse(row["path_id"].ToString());
            var coordsBlob = (byte[])row["path"];
            string coordsAsString = Encoding.UTF8.GetString(coordsBlob);
            List<Coord> coords = coordsStringToList(coordsAsString);
            paths[pathId] = coords;
        }

        con.Close();

        return paths;
    }

    public List<Coord> coordsStringToList(string s)
    {
        List<Coord> coords = new List<Coord>();
        s = s.Trim();
        var splits =  s.Split("||");
               
        for( int i = 0 ; i < splits.Length; i ++){
            string tmpSplit = splits[i];
            if(tmpSplit== "") continue;
            
            var subsplits = tmpSplit.Split(",");
                        
            double lat = double.Parse(subsplits[0].Replace('.', ','));
            double lon = double.Parse(subsplits[1].Replace('.', ','));
            double timestamp = double.Parse(subsplits[2].Replace('.', ','));
            Coord c = new Coord(lat, lon, timestamp);
            coords.Add(c);
        }
        return coords;
    }
}