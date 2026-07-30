<?php

	$host='mysql741.umbler.com';
	$username='vikkynsnorth';
	$pwd='yUZu4Q*6.t';
	$db="vikkynsnorth";

	$con=mysqli_connect($host,$username,$pwd,$db);  
			
	if(!$con){
		echo "Não Conectado";
   	}else{
		echo "Conectado com Sucesso";
   	}

	mysqli_set_charset ($con, "utf-8");

?>