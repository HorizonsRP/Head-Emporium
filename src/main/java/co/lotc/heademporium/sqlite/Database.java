package co.lotc.heademporium.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

import co.lotc.core.util.MojangCommunicator;
import co.lotc.heademporium.HeadEmporium;
import co.lotc.heademporium.HeadRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public abstract class Database {
	protected static HeadEmporium plugin;
	protected String SQLiteTableName;
	public static Connection connection;

	public Database(HeadEmporium instance, String table){
		plugin = instance;
		this.SQLiteTableName = table;
	}

	public abstract Connection getSQLConnection();

	public abstract void load();

	public abstract void setToken(int id, String catOrApprov, String nameOrReq, String texture, float priceOrAmount);

	public abstract void removeTokenByTexture(String texture);

	public String getTable() {
		return SQLiteTableName;
	}

	public void initialize(){
		connection = getSQLConnection();
		try {
			String stmt;
			stmt = "SELECT * FROM " + SQLiteTableName + ";";
			PreparedStatement ps = connection.prepareStatement(stmt);
			ResultSet rs = ps.executeQuery();
			close(ps, rs);
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, "Unable to retreive connection", ex);
		}
	}

	// Retrieve info
	public String getToken(String id, String column, boolean fromShop) {
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
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
			}
		}
		return null;
	}

	// Remove info
	public void removeToken(int id) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = getSQLConnection();
			String stmt;
			stmt = "DELETE FROM " + SQLiteTableName + " WHERE ID=" + id + ";";
			ps = conn.prepareStatement(stmt);
			ps.executeUpdate();
		} catch (SQLException ex) {
			plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
			}
		}
	}

	public static void close(PreparedStatement ps,ResultSet rs){
		try {
			if (ps != null)
				ps.close();
			if (rs != null)
				rs.close();
		} catch (SQLException ex) {
			Errors.close(plugin, ex);
		}
	}
}