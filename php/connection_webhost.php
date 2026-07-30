<?php

	$host='localhost';
	$username='id17281844_my_vikkyns_usu_apps';
	$pwd='H(_p@4YE=ikFV(FQ';
	$db="id17281844_mensagemsforappsnot";

	$con=mysqli_connect($host,$username,$pwd,$db);  
			
	if(!$con){
		echo "Não Conectado";
   	}else{
		echo "Conectado com Sucesso";
   	}

	mysqli_set_charset ($con, "utf-8");

?>