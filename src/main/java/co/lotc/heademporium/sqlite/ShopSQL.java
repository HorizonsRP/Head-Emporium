package co.lotc.heademporium.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;

import co.lotc.heademporium.HeadEmporium; // import your main class

public class ShopSQL extends Database{
	private String dbname;
	private String SQLiteTokensTable;
	private int totalId = 0;

	public ShopSQL(HeadEmporium instance){
		super(instance, "shop_table");
		dbname = instance.getConfig().getString("SQLite.Filename", "heads");
		SQLiteTokensTable = "CREATE TABLE IF NOT EXISTS " + SQLiteTableName + " (\n" +
							"    ID INTEGER PRIMARY KEY,\n" +
							"    CATEGORY TEXT NOT NULL,\n" +
							"    NAME TEXT NOT NULL,\n" +
							"    TEXTURE TEXT NOT NULL,\n" +
							"    PRICE INTEGER NOT NULL\n" +
							");";
	}


	// SQL creation stuff, You can leave the below stuff untouched.
	public Connection getSQLConnection() {
		File dataFolder = new File(plugin.getDataFolder(), dbname + ".db");
		if (!dataFolder.exists()){
			try {
				dataFolder.createNewFile();
			} catch (IOException e) {
				plugin.getLogger().log(Level.SEVERE, "File write error: " + dbname + ".db");
			}
		}
		try {
			if (connection != null && !connection.isClosed()) {
				return connection;
			}
			Class.forName("org.sqlite.JDBC");
			String locale = dataFolder.toString();
			if (HeadEmporium.DEBUGGING) {
				plugin.getLogger().info("LOCALE: " + locale);
			}
			connection = DriverManager.getConnection("jdbc:sqlite:" + locale);
			return connection;
		} catch (SQLException ex) {
			if (HeadEmporium.DEBUGGING) {
				plugin.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
			}
		} catch (ClassNotFoundException ex) {
			if (HeadEmporium.DEBUGGING) {
				plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
			}
		}
		return null;
	}

	public void load() {
		connection = getSQLConnection();
		try {
			Statement s = connection.createStatement();
			s.execute(SQLiteTokensTable);
			s.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		initialize();
	}

	public void initialize(){
		connection = getSQLConnection();
		try {
			String stmt;
			stmt = "SELECT * FROM " + SQLiteTableName + ";";
			PreparedStatement ps = connection.prepareStatement(stmt);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if (rs.getInt("ID") >= totalId) {
					totalId = rs.getInt("ID") + 1;
				}
			}
			close(ps, rs);
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, "Unable to retreive connection", ex);
		}
	}

	// Retrieve info
	public String getToken(String id, String column) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = getSQLConnection();
			String stmt;
			stmt = "SELECT * FROM " + SQLiteTableName + " WHERE ID='" + id + "';";

			ps = conn.prepareStatement(stmt);
			rs = ps.executeQuery();

			while (rs.next()) {
				if (rs.getString("ID").equalsIgnoreCase(id.toLowerCase())) {
					return rs.getString(column);
				}
			}
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
			}
		}
		return null;
	}

	// Save info
	public void setToken(int id, String catOrApprov, String nameOrReq, String texture, float priceOrAmount) {
		if (id == -1) {
			id = totalId;
		}
		Connection conn = null;
		PreparedStatement ps = null;
		String stmt;
		stmt = "INSERT OR REPLACE INTO " + SQLiteTableName + " (ID,CATEGORY,NAME,TEXTURE,PRICE) VALUES(?,?,?,?,?)";

		try {
			conn = getSQLConnection();
			ps = conn.prepareStatement(stmt);
			ps.setInt(1, id);
			ps.setString(2, catOrApprov);
			ps.setString(3, nameOrReq);
			ps.setString(4, texture);
			ps.setInt(5, (int) priceOrAmount*100);
			ps.executeUpdate();
			totalId++;
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
			}
		}
	}

	// Remove info
	public void removeToken(int id) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = getSQLConnection();
			String stmt = "DELETE FROM " + SQLiteTableName + " WHERE ID=" + id + ";";
			ps = conn.prepareStatement(stmt);
			ps.executeUpdate();
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
			}
		}
	}

	// Remove by texture
	public void removeTokenByTexture(String texture) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = getSQLConnection();
			String stmt = "DELETE FROM " + SQLiteTableName + " WHERE TEXTURE=" + texture + ";";
			ps = conn.prepareStatement(stmt);
			ps.executeUpdate();
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException ex) {
				plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
			}
		}
	}

}