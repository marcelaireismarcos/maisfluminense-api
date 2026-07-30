<?php

$host='localhost';
$username='id17281844_my_vikkyns_usu_apps';
$pwd='H(_p@4YE=ikFV(FQ';
$db="id17281844_mensagemsforappsnot";

$con=mysqli_connect($host,$username,$pwd,$db) or die('Unable to connect');

if(mysqli_connect_error($con)){
	echo "Failed to Connect to Database ".mysqli_connect_error();
}


$sql="SELECT * FROM msgpacotes";


$result=mysqli_query($con,$sql);
if($result){
	while($row=mysqli_fetch_array($result))	{
		$data[]=$row;
	}
	
	print(json_encode($data));
}

mysqli_close($con);

?>