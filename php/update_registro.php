<?php
	include 'connection.php';
	
	if( !isset($_POST['registro']) ) {
	    throw new \InvalidArgumentException( "Incorrect data" );
	} 

	$pacote = $_POST['pacote'];
	$registro = $_POST['registro'];

	$query = mysqli_query($con, "UPDATE msgpacotes SET registro = '$registro' WHERE pacote = '$pacote' ");

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
