<?php

	$host='localhost';
	$username='vikkynsnorth';
	$pwd='jaMeG:1971c1986';
	$db="vikkynsnorth";

	$con=mysqli_connect($host,$username,$pwd,$db);  
			
	if(!$con){
		echo "Não Conectado";
   	}else{
		echo "Conectado com Sucesso";
   	}

	mysqli_set_charset ($con, "utf-8");

?>