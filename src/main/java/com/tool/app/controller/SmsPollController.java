package com.tool.app.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;
import com.tool.app.bean.Moderator;
import com.tool.app.bean.Moderator.EmailValidator;
import com.tool.app.bean.Moderator.ModValidator;
import com.tool.app.bean.Poll;



@RestController
@RequestMapping("/api/v1")
public class SmsPollController {

	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	private static int moderatorSeqId = 10000;
	private static long pollSeqId = 345546880; 
	private static HashMap<Integer,Moderator> modMap = new HashMap<Integer,Moderator>();
	private static HashMap<String,Poll> pollMap = new HashMap<String,Poll>();
	private static HashMap<Integer,ArrayList<String>> modPollMap = new HashMap<Integer,ArrayList<String>>();



	/**
	 * to create moderator : POST
	 * @param mod
	 * @param result
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value="/moderators",produces = "application/json")
	public ResponseEntity<String> createModerator(@Validated({ModValidator.class}) @RequestBody Moderator mod, BindingResult result)
	{
		if(result.hasErrors())
		{
			return new ResponseEntity<String>(callError(result),HttpStatus.BAD_REQUEST);
		}
		mod.setId(moderatorSeqId);
		mod.setCreated_at(dateFormatter.format(new Date()));
		modMap.put(moderatorSeqId, mod);
		moderatorSeqId++;
		return new ResponseEntity<String>(mod.toString(),HttpStatus.CREATED);
	}



	/**
	 * to get moderator details : GET
	 * @param moderatorId
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value="/moderators/{moderatorId}")
	public ResponseEntity<Moderator> getModeratorDetails(@PathVariable int moderatorId)
	{
		if(modMap.containsKey(moderatorId))
			return new ResponseEntity<Moderator>(modMap.get(moderatorId),HttpStatus.OK);
		else
			return new ResponseEntity<Moderator>(HttpStatus.NOT_FOUND);

	}


	/**
	 * to update moderator details : PUT
	 * @param mod
	 * @param moderator_id
	 * @param result
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, value="/moderators/{moderator_id}" ,produces = "application/json")
	public ResponseEntity<String> updateModeratorDetails(@Validated({ EmailValidator.class }) @RequestBody Moderator mod, BindingResult result, @PathVariable int moderator_id )
	{
		if(result.hasErrors())
		{
			System.out.println("coming");
			return new ResponseEntity<String>(callError(result),HttpStatus.BAD_REQUEST);
		}
		else
		{
			if(modMap.containsKey(moderator_id))
			{
				Moderator obj  = modMap.get(moderator_id);
				obj.setEmail(mod.getEmail());
				obj.setPassword(mod.getPassword());
				return new ResponseEntity<String>(obj.toString(),HttpStatus.OK);
			}
			else 
				return new ResponseEntity<String>("Mpderator Id not found!!!",HttpStatus.NOT_FOUND);
		}
	}


	/**
	 * to create Poll : POST
	 * @param poll
	 * @param moderator_id
	 * @param result
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value="/moderators/{moderator_id}/polls" ,produces = "application/json")
	public ResponseEntity<String> createPoll(@Valid @RequestBody Poll poll, BindingResult result, @PathVariable int moderator_id)
	{
		if(result.hasErrors())
		{
			return new ResponseEntity<String>(callError(result),HttpStatus.BAD_REQUEST);
		}
		else
		{
			if(modMap.containsKey(moderator_id))
			{
				poll.setId(Long.toString(pollSeqId, 36).toUpperCase());
				int choiceLen = poll.getChoice().length;
				int[] results = new int[choiceLen];
				poll.setResults(results);
				pollMap.put(poll.getId() , poll);

				if(!modPollMap.containsKey(moderator_id))
				{
					ArrayList<String> pollList = new ArrayList<String>();
					modPollMap.put(moderator_id, pollList);
				}
				modPollMap.get(moderator_id).add(poll.getId());

				pollSeqId++;
				return new ResponseEntity<String>(poll.toString(),HttpStatus.CREATED);
			}
			else{
				return new ResponseEntity<String>("Given Moderator Id is not found",HttpStatus.BAD_REQUEST);
			}
		}

	}

	/**
	 * to get poll details with results : GET
	 * @param moderator_id
	 * @param poll_id
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value="/moderators/{moderator_id}/polls/{poll_id}")
	public ResponseEntity<Poll> getPollDetailsWithResult(@PathVariable int moderator_id,@PathVariable String poll_id)
	{
		if(pollMap.containsKey(poll_id) && modMap.containsKey(moderator_id))
		{
			if(checkPollModeratorMapping(moderator_id,poll_id))
				return new ResponseEntity<Poll>(pollMap.get(poll_id),HttpStatus.OK);
			else
				return new ResponseEntity<Poll>(HttpStatus.BAD_REQUEST);
		}
		else
			return new ResponseEntity<Poll>(HttpStatus.NOT_FOUND);

	}

	/**
	 * to get all polls for a moderator : GET
	 * @param moderator_id
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, value="/moderators/{moderator_id}/polls")
	public ResponseEntity<ArrayList<Poll>> listAllPoll(@PathVariable int moderator_id)
	{
		ArrayList<Poll> pollList = new ArrayList<Poll>();
		if(modMap.containsKey(moderator_id) && modPollMap.containsKey(moderator_id))
		{
			ArrayList<String> pollIdList = modPollMap.get(moderator_id);
			for(String poll_id : pollIdList)
				pollList.add(pollMap.get(poll_id));
			return new ResponseEntity<ArrayList<Poll>>(pollList,HttpStatus.OK);
		}
		else
			return new ResponseEntity<ArrayList<Poll>>(HttpStatus.NOT_FOUND);
	}

	/**
	 * to delete given poll : DELETE
	 * @param moderator_id
	 * @param poll_id
	 * @return
	 */
	@RequestMapping(method = RequestMethod.DELETE, value="/moderators/{moderator_id}/polls/{poll_id}")
	public ResponseEntity<Poll> deletePoll(@PathVariable int moderator_id,@PathVariable String poll_id)
	{
		if(pollMap.containsKey(poll_id) && modMap.containsKey(moderator_id))
		{
			if(checkPollModeratorMapping(moderator_id,poll_id))
			{
				pollMap.remove(poll_id);
				modPollMap.get(moderator_id).remove(poll_id);	
				if(modPollMap.get(moderator_id).isEmpty())
				{
					modPollMap.remove(moderator_id);
				}
			}
			else
				return new ResponseEntity<Poll>(HttpStatus.BAD_REQUEST);
		}
		else
			return new ResponseEntity<Poll>(HttpStatus.NOT_FOUND);
		return new ResponseEntity<Poll>(HttpStatus.NO_CONTENT);
	}

	/**
	 * to update poll result : PUT
	 * @param poll_id
	 * @param choice
	 * @return
	 */
	@RequestMapping(method = RequestMethod.PUT, value="/polls/{poll_id}",produces = "application/json")
	public ResponseEntity<String> updatePoll(@PathVariable String poll_id, @RequestParam int choice)
	{
			if(pollMap.containsKey(poll_id))
			{
				int[] result = pollMap.get(poll_id).getResults();
				if(choice < result.length ){
					result[choice]++;
					pollMap.get(poll_id).setResults(result);
					return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
				}
				else
					return new ResponseEntity<String>("Choice index is invalid!!!",HttpStatus.BAD_REQUEST);
			}
			else
				return new ResponseEntity<String>(HttpStatus.NOT_FOUND);

	}

	/**
	 * to get poll details without results : GET
	 * @param poll_id
	 * @return
	 */
	@JsonView(Poll.ViewPoll.class)
	@RequestMapping(method = RequestMethod.GET, value="/polls/{poll_id}")
	public ResponseEntity<Poll> getPollDetailsWithoutResults(@PathVariable String poll_id)
	{
		if(pollMap.containsKey(poll_id)){
			Poll poll = pollMap.get(poll_id);
			return new ResponseEntity<Poll>(poll,HttpStatus.OK);
		}
		else
			return new ResponseEntity<Poll>(HttpStatus.NOT_FOUND);	
	}


	/**
	 * to check if pollId is mapped with moderator id
	 * @param modId
	 * @param poll_id
	 * @return
	 */
	private boolean checkPollModeratorMapping(int modId, String poll_id) {
		return modPollMap.get(modId).contains(poll_id);
	}

	/**
	 * to create error string
	 * @param result
	 * @return
	 */
	private String callError(BindingResult result) {
		StringBuilder errorMsg = new StringBuilder();
		for (ObjectError err: result.getAllErrors()){
			errorMsg.append(err.getDefaultMessage());
		}
		return errorMsg.toString();

	}
	
	/*
	@RequestMapping(method = RequestMethod.GET, value="/polls/{poll_id}")
	public ResponseEntity<String> getPollDetailsWithoutResult(@PathVariable String poll_id)
	{
		JSONSerializer serializer = new JSONSerializer();
		String jsonString = null;
		try{
			if(pollMap.containsKey(poll_id)){
		    	Poll poll = pollMap.get(poll_id);
		    	jsonString = serializer.exclude("*.class").exclude("results").serialize( poll );
				}
			else
				return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return new ResponseEntity<String>(jsonString,HttpStatus.OK);
	}	*/


}
