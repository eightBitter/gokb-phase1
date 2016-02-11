
<h3> <input type="radio" name="prob_res_${prob.problemSequence}" value="newTitleInTHG"/> Option 1 : Create a new title</h3>
<p>Use this option to create a new title - optionally using the correct identifier. Link the new title to the
title history of the title identified so that we can match easily in the future. If you know dates for the new title,
suggest them in the form below, otherwise accept the defaults. 
<select name="prob_res_newtitle_placement_${prob.problemSequence}">
  <option value="AddPre">Add as preceeding</option>
  <option value="AddPro">Add as proceeding</option>
</select>
Using <input type="date" name="prob_res_newtitle_date_${prob.problemSequence}"></input>
</p>

<h3> <input type="radio" name="prob_res_${prob.problemSequence}" value="variantName"/> Option 2 : Add the new title string as a variant</h3>
<p>The supplied title really is just a radically different variant of the canonical one - add it as a variant to
resolve this issue.</p>
</select>
