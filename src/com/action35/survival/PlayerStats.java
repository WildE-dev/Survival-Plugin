package com.action35.survival;

public class PlayerStats {

	public long loginTime;
	public int mobKills = 0;
	public int deaths = 0;
	public int blocksPlaced = 0;
	public int blocksBroken = 0;
	
	public PlayerStats() {
		loginTime = System.currentTimeMillis();
	}
	
	public PlayerStats incrementMobKills() {
		mobKills++;
		return this;
	}
	
	public PlayerStats incrementDeaths() {
		deaths++;
		return this;
	}
	
	public PlayerStats incrementBlocksPlaced() {
		blocksPlaced++;
		return this;
	}
	
	public PlayerStats incrementBlocksBroken() {
		blocksBroken++;
		return this;
	}
}
