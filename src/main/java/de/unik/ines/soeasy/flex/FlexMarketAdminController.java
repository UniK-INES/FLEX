/**
 * This file is part of INES FLEX - 
 * INES (Integrated Energy Systems) FLexibility Energy eXchange
 * 
 * INES FLEX is free software: You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * INES FLEX is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020
 * Department of Integrated Energy Systems, University of Kassel, Kassel, Germany
 */
package de.unik.ines.soeasy.flex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.soeasy.common.model.TimeInformation;
import de.unik.ines.soeasy.flex.balance.MeterReadingManager;
import de.unik.ines.soeasy.flex.clearing.MarketProductClearer;
import de.unik.ines.soeasy.flex.clearing.uniform.UniformPriceClearing;
import de.unik.ines.soeasy.flex.exceptions.DuplicateUserException;
import de.unik.ines.soeasy.flex.model.MMarketProductPattern;
import de.unik.ines.soeasy.flex.model.Role;
import de.unik.ines.soeasy.flex.model.UserAccount;
import de.unik.ines.soeasy.flex.repos.ClearingInfoRepository;
import de.unik.ines.soeasy.flex.repos.FlexOfferRepository;
import de.unik.ines.soeasy.flex.repos.FlexOrderRepository;
import de.unik.ines.soeasy.flex.repos.GridDataRepository;
import de.unik.ines.soeasy.flex.repos.GridFlexDemandSmdRepos;
import de.unik.ines.soeasy.flex.repos.MMarketProductRepository;
import de.unik.ines.soeasy.flex.repos.MarketEnergyRequestRepository;
import de.unik.ines.soeasy.flex.repos.MarketMeterReadingRepository;
import de.unik.ines.soeasy.flex.repos.MarketProductRepository;
import de.unik.ines.soeasy.flex.repos.RoleRepository;
import de.unik.ines.soeasy.flex.repos.UserAccountRepository;
import de.unik.ines.soeasy.flex.scheduling.TimeInitializingBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author Sascha Holzhauer
 *
 */
@RestController()
@RequestMapping("admin")
@Api(value = "Flex Market Administration Controller")
public class FlexMarketAdminController {

	private Log log = LogFactory.getLog(FlexMarketAdminController.class);

	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	@Autowired
	UserAccountRepository userRepos;
	
	@Autowired
	RoleRepository roleRepos;
	
	@Autowired
	MMarketProductRepository mmProductRepos;
	
	@Autowired
	MarketProductRepository mProductRepos;


	@Autowired
	GridFlexDemandSmdRepos gridFlexDemandRepos;

	@Autowired
	FlexOfferRepository fofferRepos;

	@Autowired
	FlexOrderRepository forderRepos;
		
	@Autowired
	MarketEnergyRequestRepository requestRepos;
	
	@Autowired
	ClearingInfoRepository clearingRepos;
	
	@Autowired
	GridDataRepository gridDataRepos;

	@Autowired
	MarketMeterReadingRepository meterReadinRepos;

	@Autowired
	UniformPriceClearing clearing;
	
	@Autowired
	MarketProductClearer mpClearer;
	
	@Autowired
	FlexMarketInfoController marketController;
	
	@Autowired
	ConfigurableApplicationContext appContext;
	
	@Autowired
	TimeInitializingBean timebean;
	
	@Autowired
	MeterReadingManager readingManager;
	
	
	@Value("${db.dump.targetdir:./}")
	protected String db_dumpdir;
	
	@Value("${db.dump.filename:DEX}")
	protected String db_dumpfilename;
	
	@Value("${db.dump.port:5432}")
	protected String db_port;
	
	@Value("${db.dump.database:enavi}")
	protected String db_database;
	
	@Value("${spring.datasource.username:DEXuser}")
	protected String db_username;

	@Value("${spring.datasource.password:DEXpass}")
	protected String db_password;
	
	@Value("${de.unik.ines.soeasy.flex.autostart:false}")
	protected boolean autostart;

    public FlexMarketAdminController(BCryptPasswordEncoder bCryptPasswordEncoder) {
    	this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }
    
    @Autowired
    private DataSource dataSource;

	/*****************************
	 * Start/Stop Server
	 *****************************/
	
	@ApiOperation(value = "Schedules clearing of market products.", response = String.class)
	@RequestMapping(value="/start", method = RequestMethod.GET)
	@CrossOrigin
	public String startMarket(){
		log.info("Starting FLEX server...");
		
		try {
			timebean.applyChanges();
			readingManager.initProperties();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (MMarketProductPattern mmProduct : mmProductRepos.findAll()) {
			mpClearer.schedule(mmProduct);
		}
		
		timebean.setServerStarted();
		return "Start up done (clearings scheduled).";
	}

	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
		if (autostart) {
			this.startMarket();
			log.info("Autostart market...");
		}
	}

	@ApiOperation(value = "Stops scheduling the clearing of market products.", response = String.class)
	@RequestMapping(value="/stop", method = RequestMethod.GET)
	@CrossOrigin // required by web controller (angular2)
	public String stopMarket(){
		for (MMarketProductPattern mmProductPattern : mmProductRepos.findAll()) {
			mmProductPattern.stopClearing();
		}
		return "Scheduling of clearings stopped.";
	}
	
	@ApiOperation(value = "Activates and schedules clearing of a certain market product.", response = String.class)
	@RequestMapping(value="/startProduct", method = RequestMethod.GET)
	@CrossOrigin // required by web controller (angular2)
	public String startMarketProduct(Integer id){
		MMarketProductPattern mppattern = mmProductRepos.findById(id).get();
		mppattern.setActive();
		mpClearer.schedule(mppattern);
		return "Activation and scheduling done for product " + mppattern + " (clearings scheduled).";
	}
	
	@ApiOperation(value = "Stopps clearing of a certain market product.", response = String.class)
	@RequestMapping(value="/stopProduct", method = RequestMethod.GET)
	@CrossOrigin // required by web controller (angular2)
	public String stopMarketProduct(Integer id){
		MMarketProductPattern mppattern = mmProductRepos.findById(id).get();
		mppattern.stopClearing();
		return "Scheduling stopped for product " + mppattern + ".";
	}
	
	@ApiOperation(value = "Resets the server (deleting grid demand SMD, flex offers, flex orders, grid data, "
			+ "clearing information), and restarts product patterns.", response = String.class)
	@RequestMapping(value="/reset", method = RequestMethod.GET)
	@CrossOrigin // required by web controller (angular2)
	public String resetRequestsClearings(){
		this.stopMarket();

		this.gridFlexDemandRepos.deleteAll();
		this.fofferRepos.deleteAll();
		this.forderRepos.deleteAll();
		this.gridDataRepos.deleteAll();
		this.clearingRepos.deleteAll();
		this.requestRepos.deleteAll();

		this.startMarket();
		return "Server reset.";
	}
	
	@ApiOperation(value = "Stopps and shuts down the entire server.", response = String.class)
	@RequestMapping(value="/shutdown", method = RequestMethod.POST)
	@CrossOrigin // required by web controller (angular2)
	public @ResponseBody String shutdownMarketServer(){
		log.info("Shutting down market server...");
		this.stopMarket();
		appContext.close();
		System.exit(0);
		return "Server shut down!";
	}

	/*****************************
	 * User Management
	 *****************************/

	@ApiOperation(value = "Adds a new user with Role USER", response = String.class)
	//@PreAuthorize("#oauth2.hasScope('admin')")
	@RequestMapping(value="/addUser", method = RequestMethod.POST,
		consumes = MediaType.APPLICATION_JSON_VALUE,
	    produces = MediaType.TEXT_PLAIN_VALUE)
	@CrossOrigin // required by web controller (angular2)
	public @ResponseBody String addUser(@RequestBody UserAccount account) {
		if (userRepos.findByName(account.getName()) != null) {
			throw new DuplicateUserException("Duplicate User", account.getName());
		}
		Collection<Role> roles = new ArrayList<>();
		roles.add(roleRepos.findByName(Role.USER));
		account.setPassword(bCryptPasswordEncoder.encode(account.getPassword()));
		account.setPassword(account.getPassword());
		account.setRoles(roles);
		userRepos.save(account);
		return account.toString();
	}
	
	@ApiOperation(value = "Adds a new user with given role", response = String.class)
	@RequestMapping(value="/addUserWithRole", method = RequestMethod.GET)
	@CrossOrigin // required by web controller (angular2)
	public String addUserWithRole(String username, String password, String role) {
		if (userRepos.findByName(username) != null) {
			throw new DuplicateUserException("Duplicate User", username);
		}
		Collection<Role> roles = new ArrayList<>();
		roles.add(roleRepos.findByName(role));
		UserAccount account = new UserAccount(username, roles);
		account.setPassword(bCryptPasswordEncoder.encode(password));
		account.setPassword(password);
		account.setRoles(roles);
		userRepos.save(account);
		return account.toString();
	}
	
	/*****************************
	 * Database Management
	 *****************************/
	
	@ApiOperation(value = "Executes given SQL script in folder sql", response = String.class)
	@RequestMapping(value="/initDb", method = RequestMethod.GET)
	public String initDb(String sqlfilename) throws ScriptException, SQLException {
		ClassPathResource sqlfile = new ClassPathResource("sql/" + sqlfilename);
		if (!sqlfile.exists()) {
			throw new IllegalArgumentException("Given sql script does not exist!");
		}
		ScriptUtils.executeSqlScript(dataSource.getConnection(), sqlfile);
		return "SQL data load successfully!";
	}
	
	@ApiOperation(value = "Dumps DB to file", response = String.class)
	@RequestMapping(value="/dumpdb", method = RequestMethod.GET)
	public String dumpDB() throws IOException, InterruptedException {
	    Process p;
	    ProcessBuilder pb;
	    String file =  this.db_dumpdir + "/" + this.db_dumpfilename + new SimpleDateFormat("yyyyMMddHHmm").format(new Date()) + ".sql";
	    pb = new ProcessBuilder(
	            "pg_dump",
	            "--host", "localhost",
	            "--port", "5432",
	            "--username", this.db_username,
	            "--no-password",
	            "--format", "custom",
	            "--blobs",
	            "--verbose", "--file", file, db_database);
	    try {
	        final Map<String, String> env = pb.environment();
	        boolean error_occured = false;
	        env.put("PGPASSWORD", this.db_password);
	        p = pb.start();
	        final BufferedReader r = new BufferedReader(
	                new InputStreamReader(p.getErrorStream()));
	        String line = r.readLine();
	        while (line != null) {
	        	if (line.contains("failed") || line.contains("ERROR") || line.contains("FATAL")) {
	        		log.error(line);
		            error_occured = true;
	        	} else {
	        		log.debug(line);
	        	}
	            line = r.readLine();
	        }
	        r.close();
	        p.waitFor();
	        log.info("Exit value:" + p.exitValue());
	        if (error_occured) {
	        	 return "DB dump to " + file + " failed (error code " + p.exitValue() + ") !";
	        } else {
	        	return "DB successfully dumped to " + file + "!";
	        }

	    } catch (IOException | InterruptedException e) {
	        log.error(e.getMessage());
	        return "DB dumped to " + file + " failed!";
	    }
	}
	
	@ApiOperation(value = "Duplicate database", response = String.class)
	@RequestMapping(value="/dublicatedb", method = RequestMethod.GET)
	public String dublicateDB(String postfix) throws IOException, InterruptedException, ScriptException, SQLException {
		dataSource.getConnection().nativeSQL("CREATE DATABASE enavi" + postfix + " TEMPLATE " + this.db_database + ";");
		return "Database successfully dublicated!";
	}
	
	/*****************************
	 * Configuration
	 *****************************/

	/**
	 * NOTE: When offset, basetime, or time factor are decreased, the FLEX market is
	 * being reset in order to avoid overlapping of old and new submissions (in
	 * these cases, certain time would be passed trough more than once).
	 * 
	 * @param basetime
	 * @param offset
	 * @param timeFactor
	 * @return
	 */
	@ApiOperation(value = "Configure time management", response = Map.class)
	@RequestMapping(value = "/timing", method = RequestMethod.GET)
	public TimeInformation setTiming(@RequestParam(name = "bt", defaultValue = "" + Long.MIN_VALUE) long basetime,
			@RequestParam(name = "os", defaultValue = "" + Long.MIN_VALUE) long offset,
			@RequestParam(name = "tf", defaultValue = "" + Double.NaN) double timeFactor) {
		TimeInformation tinfo = this.timebean.getTimeInformation(this.mProductRepos, this.readingManager);
		boolean valueChanged = false;
		if (offset != Long.MIN_VALUE && offset != tinfo.offset) {
			this.timebean.setOffset(offset);
			// overlapping of old and new submissions needs to be avoided:
			if (offset < tinfo.offset) {
				log.info("Reset market because offset has been decreased!");
				this.resetRequestsClearings();
			}
			valueChanged = true;
		}
		
		if (basetime != Long.MIN_VALUE && basetime != tinfo.baseTime) {
			this.timebean.setBasetime(basetime);
			// overlapping of old and new submissions needs to be avoided:
			if (basetime < tinfo.baseTime) {
				log.info("Reset market because baseTime has been decreased!");
				this.resetRequestsClearings();
			}
			valueChanged = true;
		}
		
		if (timeFactor != Double.NaN && timeFactor != tinfo.simulationFactor) {
			this.timebean.setFactor(timeFactor);
			// overlapping of old and new submissions needs to be avoided:
			if (timeFactor < tinfo.simulationFactor) {
				log.info("Reset market because timeFactor has been decreased!");
				this.resetRequestsClearings();
			}
			valueChanged = true;
		}

		if (valueChanged) {
			this.timebean.applyChanges();
		}
		return this.timebean.getTimeInformation(this.mProductRepos, this.readingManager);
	}

	/*****************************
	 * Information
	 *****************************/
	
	@ApiOperation(value = "Returns backend server status information", response = Map.class)
	@RequestMapping(value="/status", method = RequestMethod.GET)
	public Map<String, String> getStatus() {
		Map<String, String> infos = new HashMap<>();

		infos.put("Server status", timebean.isServerStarted() ? "Started" : "Not started");
		infos.put("Number of users", Long.toString(userRepos.count()));
		infos.put("Number of products", Long.toString(mmProductRepos.count()));
		int numActiveProducts = 0;
		for (MMarketProductPattern mmProductPattern : mmProductRepos.findAll()) {
			if (mmProductPattern.isActive()) {
				numActiveProducts++;
			}
		}
		infos.put("Number of active products", numActiveProducts + "");
		infos.put("Number of demand requests", Long.toString(gridFlexDemandRepos.count()));
		infos.put("Number of flex offers", Long.toString(fofferRepos.count()));
		infos.put("Number of flex orders", Long.toString(forderRepos.count()));
		return infos;
	}
}
