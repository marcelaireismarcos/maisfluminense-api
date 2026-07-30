<?php
	include 'connection.php';

	$pacote = $_POST['pacote'];
	$mensagem = $_POST['mensagem'];
	$link = $_POST['link'];
	$registro = $_POST['registro'];
	$response = array();

	  $query = mysqli_query($con, "INSERT INTO msgpacotes (pacote, mensagem, link, registro) VALUES ('$pacote','$mensagem','$link','$registro')");

	  if (!$query) {
	     printf("ERRO AQUI ==== Error: %s\n", mysqli_error($con));
	     exit();
	  }

	  if($query){
	    $response['success'] = 'true';
	    $response['message'] = 'Dados Adicionados com Sucesso';
	  }else{
	    $response['success'] = 'false';
	    $response['message'] = 'Falha ao tentar Adicionar Dados';
	  }

	echo json_encode($response);

	mysqli_close($con);
?>
