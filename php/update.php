<?php
	include 'connection.php';
	
	if( !isset($_POST['mensagem'], $_POST['link']) ) {
	    throw new \InvalidArgumentException( "Incorrect data" );
	} 

	$pacote = $_POST['pacote'];
	$mensagem = $_POST['mensagem'];
	$link = $_POST['link'];

	$query = mysqli_query($con, "UPDATE msgpacotes SET mensagem = '$mensagem', link = '$link' WHERE pacote = '$pacote' ");

	if($query){
	  $response['success'] = 'true';
	  $response['message'] = 'Dados ATUALIZADOS com Sucesso';
	}else{
	  $response['success'] = 'false';
	  $response['message'] = 'Falha ao tentar ATUALIZAR os Dados';
	}

	echo json_encode($response);

	mysqli_close($con);
?>
